package be.digitalia.fosdem.db

import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.room.*
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.model.*
import be.digitalia.fosdem.utils.DateUtils
import java.util.*

@Dao
abstract class ScheduleDao(private val appDatabase: AppDatabase) {

    private val lastUpdateTime = MutableLiveData<Long?>()

    /**
     * @return The last update time in milliseconds since EPOCH, or -1 if not available.
     * This LiveData is pre-initialized with the up-to-date value.
     */
    @MainThread
    fun getLastUpdateTime(): LiveData<Long?> {
        if (lastUpdateTime.value == null) {
            lastUpdateTime.value = appDatabase.sharedPreferences.getLong(LAST_UPDATE_TIME_PREF, -1L)
        }
        return lastUpdateTime
    }

    /**
     * @return The time identifier of the current version of the database.
     */
    val lastModifiedTag: String?
        get() = appDatabase.sharedPreferences.getString(LAST_MODIFIED_TAG_PREF, null)

    private class EmptyScheduleException : Exception()

    /**
     * Stores the schedule in the database.
     *
     * @param events The events stream.
     * @return The number of events processed.
     */
    @WorkerThread
    fun storeSchedule(events: Iterable<DetailedEvent>, lastModifiedTag: String?): Int {
        val totalEvents = try {
            storeScheduleInternal(events, lastModifiedTag)
        } catch (ese: EmptyScheduleException) {
            0
        }
        if (totalEvents > 0) { // Set last update time and server's last modified tag
            val now = System.currentTimeMillis()
            appDatabase.sharedPreferences.edit()
                    .putLong(LAST_UPDATE_TIME_PREF, now)
                    .putString(LAST_MODIFIED_TAG_PREF, lastModifiedTag)
                    .apply()
            lastUpdateTime.postValue(now)

            FosdemAlarmManager.onScheduleRefreshed()
        }
        return totalEvents
    }

    @Transaction
    protected open fun storeScheduleInternal(events: Iterable<DetailedEvent>, lastModifiedTag: String?): Int {
        // 1: Delete the previous schedule
        clearSchedule()

        // 2: Insert the events
        var totalEvents = 0
        val tracks = mutableMapOf<Track, Long>()
        var nextTrackId = 0L
        var minEventId = Long.MAX_VALUE
        val days: MutableSet<Day> = HashSet(2)

        for ((event, details) in events) {
            // Retrieve or insert Track
            val track = event.track
            var trackId = tracks[track]
            if (trackId == null) {
                // New track
                trackId = ++nextTrackId
                val newTrack = Track(trackId, track.name, track.type)
                insertTrack(newTrack)
                tracks[newTrack] = trackId
            }

            val eventId = event.id
            try {
                // Insert main event and fulltext fields
                val eventEntity = EventEntity(
                        eventId,
                        event.day.index,
                        event.startTime,
                        event.endTime,
                        event.roomName,
                        event.slug,
                        trackId,
                        event.abstractText,
                        event.description
                )
                val eventTitles = EventTitles(
                        eventId,
                        event.title,
                        event.subTitle
                )
                insertEvent(eventEntity, eventTitles)
            } catch (e: Exception) {
                // Duplicate event: skip
                continue
            }

            days.add(event.day)
            if (eventId < minEventId) {
                minEventId = eventId
            }

            val persons = details.persons
            insertPersons(persons)
            val eventsToPersons = Array(persons.size) {
                EventToPerson(eventId, persons[it].id)
            }
            insertEventsToPersons(eventsToPersons)

            insertLinks(details.links)

            totalEvents++
        }

        if (totalEvents == 0) {
            // Rollback the transaction
            throw EmptyScheduleException()
        }

        // 3: Insert collected days
        insertDays(days)

        // 4: Purge outdated bookmarks
        purgeOutdatedBookmarks(minEventId)

        return totalEvents
    }

    @Insert
    protected abstract fun insertTrack(track: Track)

    @Insert
    protected abstract fun insertEvent(eventEntity: EventEntity, eventTitles: EventTitles)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insertPersons(persons: List<Person>)

    @Insert
    protected abstract fun insertEventsToPersons(eventsToPersons: Array<EventToPerson>)

    @Insert
    protected abstract fun insertLinks(links: List<Link>)

    @Insert
    protected abstract fun insertDays(days: Set<Day>)

    @Query("DELETE FROM bookmarks WHERE event_id < :minEventId")
    protected abstract fun purgeOutdatedBookmarks(minEventId: Long)

    @WorkerThread
    @Transaction
    open fun clearSchedule() {
        clearEvents()
        clearEventTitles()
        clearPersons()
        clearEventToPersons()
        clearLinks()
        clearTracks()
        clearDays()
    }

    @Query("DELETE FROM events")
    protected abstract fun clearEvents()

    @Query("DELETE FROM events_titles")
    protected abstract fun clearEventTitles()

    @Query("DELETE FROM persons")
    protected abstract fun clearPersons()

    @Query("DELETE FROM events_persons")
    protected abstract fun clearEventToPersons()

    @Query("DELETE FROM links")
    protected abstract fun clearLinks()

    @Query("DELETE FROM tracks")
    protected abstract fun clearTracks()

    @Query("DELETE FROM days")
    protected abstract fun clearDays()

    // Cache days
    private val daysLiveDataDelegate = lazy { getDaysInternal() }

    val days: LiveData<List<Day>> by daysLiveDataDelegate

    @Query("SELECT `index`, date FROM days ORDER BY `index` ASC")
    protected abstract fun getDaysInternal(): LiveData<List<Day>>

    @WorkerThread
    fun getYear(): Int {
        var date = 0L

        // Compute from cached days if available
        val days = if (daysLiveDataDelegate.isInitialized()) days.value else null
        if (days != null) {
            if (days.isNotEmpty()) {
                date = days[0].date.time
            }
        } else {
            date = getConferenceStartDate()
        }

        // Use the current year by default
        if (date == 0L) {
            date = System.currentTimeMillis()
        }

        return DateUtils.getYear(date)
    }

    @Query("SELECT date FROM days ORDER BY `index` ASC LIMIT 1")
    protected abstract fun getConferenceStartDate(): Long

    @Query("""SELECT t.id, t.name, t.type FROM tracks t
        JOIN events e ON t.id = e.track_id
        WHERE e.day_index = :day
        GROUP BY t.id
        ORDER BY t.name ASC""")
    abstract fun getTracks(day: Day): LiveData<List<Track>>

    /**
     * Returns the event with the specified id, or null if not found.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.id = :id
        GROUP BY e.id""")
    @WorkerThread
    abstract fun getEvent(id: Long): Event?

    /**
     * Returns all found events whose id is part of the given list.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        WHERE e.id IN (:ids)
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getEvents(ids: LongArray): DataSource.Factory<Int, StatusEvent>

    /**
     * Returns the events for a specified track.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        WHERE e.day_index = :day AND e.track_id = :track
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getEvents(day: Day, track: Track): LiveData<List<StatusEvent>>

    /**
     * Returns a snapshot of the events for a specified track (without the bookmark status).
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.day_index = :day AND e.track_id = :track
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getEventsSnapshot(day: Day, track: Track): List<Event>

    /**
     * Returns events starting in the specified interval, ordered by ascending start time.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        WHERE e.start_time BETWEEN :minStartTime AND :maxStartTime
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getEventsWithStartTime(minStartTime: Long, maxStartTime: Long): DataSource.Factory<Int, StatusEvent>

    /**
     * Returns events in progress at the specified time, ordered by descending start time.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        WHERE e.start_time <= :time AND :time < e.end_time
        GROUP BY e.id
        ORDER BY e.start_time DESC""")
    abstract fun getEventsInProgress(time: Long): DataSource.Factory<Int, StatusEvent>

    /**
     * Returns the events presented by the specified person.
     */
    @Query("""SELECT e.id , e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        JOIN events_persons ep2 ON e.id = ep2.event_id
        WHERE ep2.person_id = :person
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getEvents(person: Person): DataSource.Factory<Int, StatusEvent>

    /**
     * Search through matching titles, subtitles, track names, person names.
     * We need to use an union of 3 sub-queries because a "match" condition can not be
     * accompanied by other conditions in a "where" statement.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type,
        b.event_id IS NOT NULL AS is_bookmarked
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        LEFT JOIN bookmarks b ON e.id = b.event_id
        WHERE e.id IN (
            SELECT `rowid`
            FROM events_titles
            WHERE events_titles MATCH :query || '*'
        UNION
            SELECT e.id
            FROM events e
            JOIN tracks t ON e.track_id = t.id
            WHERE t.name LIKE '%' || :query || '%'
        UNION
            SELECT ep.event_id
            FROM events_persons ep
            JOIN persons p ON ep.person_id = p.`rowid`
            WHERE p.name MATCH :query || '*'
        )
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract fun getSearchResults(query: String): DataSource.Factory<Int, StatusEvent>

    /**
     * Method called by SearchSuggestionProvider to return search results in the format expected by the search framework.
     */
    @Query("""SELECT e.id AS ${BaseColumns._ID},
        et.title AS ${SearchManager.SUGGEST_COLUMN_TEXT_1},
        IFNULL(GROUP_CONCAT(p.name, ', '), '') || ' - ' || t.name AS ${SearchManager.SUGGEST_COLUMN_TEXT_2},
        e.id AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA}
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.id IN (
            SELECT `rowid`
            FROM events_titles
            WHERE events_titles MATCH :query || '*'
        UNION
            SELECT e.id
            FROM events e
            JOIN tracks t ON e.track_id = t.id
            WHERE t.name LIKE '%' || :query || '%'
        UNION
            SELECT ep.event_id
            FROM events_persons ep
            JOIN persons p ON ep.person_id = p.`rowid`
            WHERE p.name MATCH :query || '*'
        )
        GROUP BY e.id
        ORDER BY e.start_time ASC LIMIT :limit""")
    @WorkerThread
    abstract fun getSearchSuggestionResults(query: String, limit: Int): Cursor

    /**
     * Returns all persons in alphabetical order.
     */
    @Query("""SELECT `rowid`, name
        FROM persons
        ORDER BY name COLLATE NOCASE""")
    abstract fun getPersons(): DataSource.Factory<Int, Person>

    fun getEventDetails(event: Event): LiveData<EventDetails> {
        val result = MutableLiveData<EventDetails>()
        appDatabase.queryExecutor.execute {
            result.postValue(EventDetails(getPersons(event), getLinks(event)))
        }
        return result
    }

    @Query("""SELECT p.`rowid`, p.name
        FROM persons p
        JOIN events_persons ep ON p.`rowid` = ep.person_id
        WHERE ep.event_id = :event""")
    protected abstract fun getPersons(event: Event): List<Person>

    @Query("SELECT * FROM links WHERE event_id = :event ORDER BY id ASC")
    protected abstract fun getLinks(event: Event?): List<Link>

    companion object {
        private const val LAST_UPDATE_TIME_PREF = "last_update_time"
        private const val LAST_MODIFIED_TAG_PREF = "last_modified_tag"
    }
}