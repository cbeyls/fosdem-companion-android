package be.digitalia.fosdem.db

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import be.digitalia.fosdem.db.converters.NonNullInstantTypeConverters
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.db.entities.RoomColor
import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.DetailedEvent
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.PersonDetails
import be.digitalia.fosdem.model.ScheduleSection
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.BackgroundWorkScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

@Dao
abstract class ScheduleDao(private val appDatabase: AppDatabase) {
    val version: Flow<Int> =
        appDatabase.createVersionFlow(BackgroundWorkScope, EventEntity.TABLE_NAME)
    val bookmarksVersion: Flow<Int>
        get() = appDatabase.bookmarksDao.version
    val databaseVersion
        get() = AppDatabase.VERSION

    /**
     * @return The conference id, or null if not available.
     */
    val conferenceId: Flow<String?> = appDatabase.dataStore.data.map { prefs ->
        prefs[CONFERENCE_ID_PREF_KEY]
    }

    /**
     * @return The conference title, or null if not available.
     */
    val conferenceTitle: Flow<String?> = appDatabase.dataStore.data.map { prefs ->
        prefs[CONFERENCE_TITLE_PREF_KEY]
    }

    /**
     * @return The base URL for the schedule website, or null if not available.
     */
    val baseUrl: Flow<String?> = appDatabase.dataStore.data.map { prefs ->
        prefs[BASE_URL_PREF_KEY]
    }

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
        prefs[LAST_MODIFIED_TAG_PREF_KEY]
    }

    private class EmptyScheduleException : Exception()

    /**
     * Stores the schedule in the database.
     *
     * @param schedule The streaming schedule data split in multiple sections.
     * @return The number of events processed. If 0, no data or metadata will be inserted.
     */
    suspend fun storeSchedule(schedule: Sequence<ScheduleSection>, lastModifiedTag: String?): Int {
        return try {
            appDatabase.useWriterConnection { transactor ->
                transactor.withTransaction(SQLiteTransactionType.EXCLUSIVE) {
                    var totalEvents = 0
                    var minEventId = Long.MAX_VALUE
                    var conferenceSection: ScheduleSection.Conference? = null

                    // 1: Delete the previous schedule
                    clearSchedule()

                    // 2: Store the main schedule data
                    val tracksByName = mutableMapOf<String, Track>()
                    for (section in schedule) {
                        when (section) {
                            is ScheduleSection.Conference -> conferenceSection = section
                            is ScheduleSection.Persons -> storePersonDetails(section.persons)
                            is ScheduleSection.Day -> {
                                val result = storeDay(section.day, section.events, tracksByName)
                                totalEvents += result.totalEvents
                                minEventId = minOf(minEventId, result.minEventId)
                            }
                        }
                    }

                    if (totalEvents == 0) {
                        // Trigger a transaction rollback but don't report an error
                        throw EmptyScheduleException()
                    }

                    // 3: Purge outdated bookmarks and room colors
                    purgeOutdatedBookmarks(minEventId)
                    purgeOutdatedRoomColors()

                    // 4: Store the conference data
                    checkNotNull(conferenceSection) { "Missing conference data" }
                    appDatabase.dataStore.edit { prefs ->
                        prefs.clear()
                        prefs[CONFERENCE_ID_PREF_KEY] = conferenceSection.conferenceId
                        prefs[CONFERENCE_TITLE_PREF_KEY] = conferenceSection.conferenceTitle
                        prefs[BASE_URL_PREF_KEY] = conferenceSection.baseUrl
                        prefs[LATEST_UPDATE_TIME_PREF_KEY] = System.currentTimeMillis()
                        if (lastModifiedTag != null) {
                            prefs[LAST_MODIFIED_TAG_PREF_KEY] = lastModifiedTag
                        }
                    }

                    totalEvents
                }
            }
        } catch (_: EmptyScheduleException) {
            0
        }
    }

    private suspend fun storePersonDetails(persons: Sequence<PersonDetails>) {
        for (person in persons) {
            insertPersonDetails(person)
        }
    }

    private class DayResult(
        val totalEvents: Int,
        val minEventId: Long
    )

    private suspend fun storeDay(
        day: Day,
        dayEvents: Sequence<DetailedEvent>,
        tracksByName: MutableMap<String, Track>
    ): DayResult {
        // 1: Insert the events
        var totalEvents = 0
        var minEventId = Long.MAX_VALUE

        for ((event, details) in dayEvents) {
            // Retrieve or insert Track
            val eventTrack = event.track
            val track = tracksByName.getOrPut(eventTrack.name) {
                // New track
                eventTrack.copy(id = tracksByName.size + 1L).also { insertTrack(it) }
            }

            val eventId = event.id
            try {
                // Insert main event and fulltext fields
                val eventEntity = EventEntity(
                    id = eventId,
                    dayIndex = day.index,
                    startTime = event.startTime,
                    startTimeOffset = event.startTimeOffset,
                    endTime = event.endTime,
                    roomName = event.roomName,
                    url = event.url,
                    trackId = track.id,
                    abstractText = event.abstractText,
                    description = event.description,
                    feedbackUrl = event.feedbackUrl
                )
                val eventTitles = EventTitles(
                    id = eventId,
                    title = event.title,
                    subTitle = event.subTitle
                )
                insertEvent(eventEntity, eventTitles)
            } catch (_: Exception) {
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

        // 2: Insert the day if it contains at least one event
        if (totalEvents > 0) {
            insertDay(day)
        }

        return DayResult(
            totalEvents = totalEvents,
            minEventId = minEventId
        )
    }

    @Insert
    protected abstract suspend fun insertTrack(track: Track)

    @Insert
    protected abstract suspend fun insertEvent(eventEntity: EventEntity, eventTitles: EventTitles)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPersons(persons: List<Person>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPersonDetails(person: PersonDetails)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEventsToPersons(eventsToPersons: List<EventToPerson>)

    @Insert
    protected abstract suspend fun insertAttachments(attachments: List<Attachment>)

    @Insert
    protected abstract suspend fun insertLinks(links: List<Link>)

    @Insert
    protected abstract suspend fun insertDay(days: Day)

    @Query("DELETE FROM bookmarks WHERE event_id < :minEventId")
    protected abstract suspend fun purgeOutdatedBookmarks(minEventId: Long)

    @Query("DELETE FROM room_colors WHERE room_name NOT IN (SELECT DISTINCT room_name FROM events WHERE room_name IS NOT NULL)")
    protected abstract suspend fun purgeOutdatedRoomColors()

    @Query("SELECT * FROM room_colors")
    abstract suspend fun getAllRoomColors(): List<RoomColor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRoomColor(roomColor: RoomColor)

    suspend fun clearSchedule() {
        appDatabase.useWriterConnection { transactor ->
            transactor.immediateTransaction {
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
    }

    // Cache days
    @OptIn(ExperimentalCoroutinesApi::class)
    val days: Flow<List<Day>> = appDatabase.invalidationTracker.createFlow(Day.TABLE_NAME)
        .mapLatest { getDaysInternal() }
        .stateIn(
            scope = BackgroundWorkScope,
            started = SharingStarted.Lazily,
            initialValue = null
        ).filterNotNull()

    @Query("SELECT `index`, date, start_time, end_time FROM days ORDER BY `index` ASC")
    protected abstract suspend fun getDaysInternal(): List<Day>

    suspend fun getYear(): Int? {
        // Compute from days if available
        return days.first().firstOrNull()?.date?.year
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
    @Query("SELECT * FROM events_view WHERE id = :id")
    abstract suspend fun getEvent(id: Long): Event?

    /**
     * Returns all found events whose id is part of the given list.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        WHERE ev.id IN (:ids)
        ORDER BY ev.start_time ASC""")
    abstract fun getEvents(ids: LongArray): PagingSource<Int, StatusEvent>

    /**
     * Returns the events for a specified track, including their bookmark status.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        WHERE ev.day_index = :day AND ev.track_id = :track
        ORDER BY ev.start_time ASC""")
    abstract suspend fun getEvents(day: Day, track: Track): List<StatusEvent>

    /**
     * Returns the events for a specified track, without their bookmark status.
     */
    @Query("SELECT * FROM events_view WHERE day_index = :day AND track_id = :track ORDER BY start_time ASC")
    abstract suspend fun getEventsWithoutBookmarkStatus(day: Day, track: Track): List<Event>

    /**
     * Returns events starting in the specified interval, ordered by ascending start time.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        WHERE ev.start_time BETWEEN :minStartTime AND :maxStartTime
        ORDER BY ev.start_time ASC""")
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract fun getEventsWithStartTime(minStartTime: Instant, maxStartTime: Instant): PagingSource<Int, StatusEvent>

    /**
     * Returns events in progress at the specified time, ordered by descending start time.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        WHERE ev.start_time <= :time AND :time < ev.end_time
        ORDER BY ev.start_time DESC""")
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract fun getEventsInProgress(time: Instant): PagingSource<Int, StatusEvent>

    /**
     * Returns the events presented by the specified person.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        JOIN events_persons ep2 ON ev.id = ep2.event_id
        WHERE ep2.person_id = :person
        ORDER BY ev.start_time ASC""")
    abstract fun getEvents(person: Person): PagingSource<Int, StatusEvent>

    /**
     * Search through matching titles, subtitles, track names, person names.
     * We need to use an union of 3 sub-queries because a "match" condition can not be
     * accompanied by other conditions in a "where" statement.
     */
    @Query("""SELECT ev.*, b.event_id IS NOT NULL AS is_bookmarked
        FROM events_view ev
        LEFT JOIN bookmarks b ON ev.id = b.event_id
        WHERE ev.id IN (
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
        ORDER BY ev.start_time ASC""")
    abstract fun getSearchResults(query: String): PagingSource<Int, StatusEvent>

    /**
     * Returns all persons in alphabetical order.
     */
    @Query("""SELECT `rowid`, name
        FROM persons
        ORDER BY name COLLATE NOCASE""")
    abstract fun getPersons(): PagingSource<Int, Person>

    /**
     * Returns the details of the given person, if available.
     */
    @Query("SELECT * FROM person_details WHERE `rowid` = :person")
    abstract suspend fun getPersonDetails(person: Person): PersonDetails?

    suspend fun getEventDetails(event: Event): EventDetails {
        // Load persons, attachments and links in parallel
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
        private val CONFERENCE_ID_PREF_KEY = stringPreferencesKey("conference_id")
        private val CONFERENCE_TITLE_PREF_KEY = stringPreferencesKey("conference_title")
        private val BASE_URL_PREF_KEY = stringPreferencesKey("base_url")
        private val LATEST_UPDATE_TIME_PREF_KEY = longPreferencesKey("latest_update_time")
        private val LAST_MODIFIED_TAG_PREF_KEY = stringPreferencesKey("last_modified_tag")
    }
}