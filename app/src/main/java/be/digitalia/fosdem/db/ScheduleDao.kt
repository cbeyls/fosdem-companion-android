package be.digitalia.fosdem.db

import androidx.annotation.WorkerThread
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NonNullInstantTypeConverters
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.DetailedEvent
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.BackgroundWorkScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate

@Dao
abstract class ScheduleDao(private val appDatabase: AppDatabase) {
    val version: StateFlow<Int> =
        appDatabase.createVersionFlow(EventEntity.TABLE_NAME)
    val bookmarksVersion: StateFlow<Int>
        get() = appDatabase.bookmarksDao.version

    /**
     * @return The latest update time, or null if not available.
     */
    val latestUpdateTime: Flow<Instant?> = appDatabase.dataStore.data.map { prefs ->
        prefs[LATEST_UPDATE_TIME_PREF_KEY]?.let { Instant.ofEpochMilli(it) }
    }

    /**
     * @return The time identifier of the current version of the database.
     */
    val lastModifiedTag: Flow<String?> = appDatabase.dataStore.data.map { prefs ->
        prefs[LAST_MODIFIED_TAG_PREF]
    }

    private class EmptyScheduleException : Exception()

    /**
     * Stores the schedule in the database.
     *
     * @param events The events stream.
     * @return The number of events processed.
     */
    @WorkerThread
    fun storeSchedule(events: Sequence<DetailedEvent>, lastModifiedTag: String?): Int {
        val totalEvents = try {
            storeScheduleInternal(events, lastModifiedTag)
        } catch (ese: EmptyScheduleException) {
            0
        }
        if (totalEvents > 0) { // Set last update time and server's last modified tag
            val now = Instant.now()
            runBlocking {
                appDatabase.dataStore.edit { prefs ->
                    prefs.clear()
                    prefs[LATEST_UPDATE_TIME_PREF_KEY] = now.toEpochMilli()
                    if (lastModifiedTag != null) {
                        prefs[LAST_MODIFIED_TAG_PREF] = lastModifiedTag
                    }
                }
            }
        }
        return totalEvents
    }

    @Transaction
    protected open fun storeScheduleInternal(events: Sequence<DetailedEvent>, lastModifiedTag: String?): Int {
        // 1: Delete the previous schedule
        clearSchedule()

        // 2: Insert the events
        var totalEvents = 0
        val tracks = mutableMapOf<Track, Long>()
        var nextTrackId = 0L
        var minEventId = Long.MAX_VALUE

        val days: MutableList<Day> = ArrayList(2)
        var currentDayIndex = -1

        for ((event, details) in events) {
            // Collect Day if new
            val day = event.day
            if (currentDayIndex != day.index) {
                days += day
                currentDayIndex = day.index
            }

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
                    id = eventId,
                    dayIndex = currentDayIndex,
                    startTime = event.startTime,
                    startTimeOffset = event.startTimeOffset,
                    endTime = event.endTime,
                    roomName = event.roomName,
                    slug = event.slug,
                    trackId = trackId,
                    abstractText = event.abstractText,
                    description = event.description
                )
                val eventTitles = EventTitles(
                    id = eventId,
                    title = event.title,
                    subTitle = event.subTitle
                )
                insertEvent(eventEntity, eventTitles)
            } catch (e: Exception) {
                // Duplicate event: skip
                continue
            }

            if (eventId < minEventId) {
                minEventId = eventId
            }

            val persons = details.persons
            insertPersons(persons)
            val eventsToPersons = persons.map { EventToPerson(eventId, it.id) }
            insertEventsToPersons(eventsToPersons)

            insertAttachments(details.attachments)
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insertEventsToPersons(eventsToPersons: List<EventToPerson>)

    @Insert
    protected abstract fun insertAttachments(attachments: List<Attachment>)

    @Insert
    protected abstract fun insertLinks(links: List<Link>)

    @Insert
    protected abstract fun insertDays(days: Collection<Day>)

    @Query("DELETE FROM bookmarks WHERE event_id < :minEventId")
    protected abstract fun purgeOutdatedBookmarks(minEventId: Long)

    @WorkerThread
    @Transaction
    open fun clearSchedule() {
        with(appDatabase.openHelper.writableDatabase) {
            execSQL("DELETE FROM events")
            execSQL("DELETE FROM events_titles")
            execSQL("DELETE FROM persons")
            execSQL("DELETE FROM events_persons")
            execSQL("DELETE FROM attachments")
            execSQL("DELETE FROM links")
            execSQL("DELETE FROM tracks")
            execSQL("DELETE FROM days")
        }
    }

    // Cache days
    @OptIn(ExperimentalCoroutinesApi::class)
    val days: Flow<List<Day>> = appDatabase.createVersionFlow(Day.TABLE_NAME)
        .mapLatest { getDaysInternal() }
        .stateIn(
            scope = BackgroundWorkScope,
            started = SharingStarted.Lazily,
            initialValue = null
        ).filterNotNull()

    @Query("SELECT `index`, date, start_time, end_time FROM days ORDER BY `index` ASC")
    protected abstract suspend fun getDaysInternal(): List<Day>

    suspend fun getYear(): Int {
        // Compute from days if available, fall back to current year
        val date = days.first().firstOrNull()?.date ?: LocalDate.now()
        return date.year
    }

    @Query("""SELECT t.id, t.name, t.type FROM tracks t
        JOIN events e ON t.id = e.track_id
        WHERE e.day_index = :day
        GROUP BY t.id
        ORDER BY t.name ASC""")
    abstract suspend fun getTracks(day: Day): List<Track>

    /**
     * Returns the event with the specified id, or null if not found.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.id = :id
        GROUP BY e.id""")
    abstract suspend fun getEvent(id: Long): Event?

    /**
     * Returns all found events whose id is part of the given list.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    abstract fun getEvents(ids: LongArray): PagingSource<Int, StatusEvent>

    /**
     * Returns the events for a specified track, including their bookmark status.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    abstract suspend fun getEvents(day: Day, track: Track): List<StatusEvent>

    /**
     * Returns the events for a specified track, without their bookmark status.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.day_index = :day AND e.track_id = :track
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    abstract suspend fun getEventsWithoutBookmarkStatus(day: Day, track: Track): List<Event>

    /**
     * Returns events starting in the specified interval, ordered by ascending start time.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract fun getEventsWithStartTime(minStartTime: Instant, maxStartTime: Instant): PagingSource<Int, StatusEvent>

    /**
     * Returns events in progress at the specified time, ordered by descending start time.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract fun getEventsInProgress(time: Instant): PagingSource<Int, StatusEvent>

    /**
     * Returns the events presented by the specified person.
     */
    @Query("""SELECT e.id , e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    abstract fun getEvents(person: Person): PagingSource<Int, StatusEvent>

    /**
     * Search through matching titles, subtitles, track names, person names.
     * We need to use an union of 3 sub-queries because a "match" condition can not be
     * accompanied by other conditions in a "where" statement.
     */
    @Query("""SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type, b.event_id IS NOT NULL AS is_bookmarked
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
    abstract fun getSearchResults(query: String): PagingSource<Int, StatusEvent>

    /**
     * Returns all persons in alphabetical order.
     */
    @Query("""SELECT `rowid`, name
        FROM persons
        ORDER BY name COLLATE NOCASE""")
    abstract fun getPersons(): PagingSource<Int, Person>

    suspend fun getEventDetails(event: Event): EventDetails {
        // Load persons and links in parallel
        return coroutineScope {
            val persons = async { getPersons(event) }
            val attachments = async { getAttachments(event) }
            val links = async { getLinks(event) }
            EventDetails(
                persons = persons.await(),
                attachments = attachments.await(),
                links = links.await()
            )
        }
    }

    @Query("""SELECT p.`rowid`, p.name
        FROM persons p
        JOIN events_persons ep ON p.`rowid` = ep.person_id
        WHERE ep.event_id = :event""")
    protected abstract suspend fun getPersons(event: Event): List<Person>

    @Query("SELECT * FROM attachments WHERE event_id = :event ORDER BY id ASC")
    protected abstract suspend fun getAttachments(event: Event?): List<Attachment>

    @Query("SELECT * FROM links WHERE event_id = :event ORDER BY id ASC")
    protected abstract suspend fun getLinks(event: Event?): List<Link>

    companion object {
        private val LATEST_UPDATE_TIME_PREF_KEY = longPreferencesKey("latest_update_time")
        private val LAST_MODIFIED_TAG_PREF = stringPreferencesKey("last_modified_tag")
    }
}