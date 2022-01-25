package be.digitalia.fosdem.db

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NonNullInstantTypeConverters
import be.digitalia.fosdem.db.entities.Bookmark
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.model.AlarmInfo
import be.digitalia.fosdem.model.Event
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

@Dao
abstract class BookmarksDao(appDatabase: AppDatabase) {

    val version: StateFlow<Int> = appDatabase.createVersionFlow(
        EventEntity.TABLE_NAME, Bookmark.TABLE_NAME
    )

    /**
     * Returns the bookmarks.
     *
     * @param minStartTime When greater than Instant.EPOCH, only return the events starting after this time.
     */
    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type
        FROM bookmarks b
        JOIN events e ON b.event_id = e.id
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        WHERE e.start_time > :minStartTime
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract fun getBookmarks(minStartTime: Instant): LiveData<List<Event>>

    @Query("""SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description,
        GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type
        FROM bookmarks b
        JOIN events e ON b.event_id = e.id
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        GROUP BY e.id
        ORDER BY e.start_time ASC""")
    @WorkerThread
    abstract fun getBookmarks(): List<Event>

    @Query("""SELECT b.event_id, e.start_time
        FROM bookmarks b
        JOIN events e ON b.event_id = e.id
        WHERE e.start_time > :minStartTime
        ORDER BY e.start_time ASC""")
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract suspend fun getBookmarksAlarmInfo(minStartTime: Instant): List<AlarmInfo>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE event_id = :event")
    abstract suspend fun getBookmarkStatus(event: Event): Boolean

    suspend fun addBookmark(event: Event): AlarmInfo? {
        val ids = addBookmarksInternal(listOf(Bookmark(event.id)))
        return if (ids[0] != -1L) AlarmInfo(event.id, event.startTime) else null
    }

    @Transaction
    open suspend fun addBookmarks(eventIds: LongArray): List<AlarmInfo> {
        // Get AlarmInfos first to filter out non-existing items
        val alarmInfos = getAlarmInfos(eventIds)
        alarmInfos.isNotEmpty() || return emptyList()

        val ids = addBookmarksInternal(alarmInfos.map { Bookmark(it.eventId) })
        // Filter out items that were already in bookmarks
        return alarmInfos.filterIndexed { index, _ -> ids[index] != -1L }
    }

    @Query("""SELECT id as event_id, start_time
        FROM events
        WHERE id IN (:ids)
        ORDER BY start_time ASC""")
    protected abstract suspend fun getAlarmInfos(ids: LongArray): List<AlarmInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addBookmarksInternal(bookmarks: List<Bookmark>): LongArray

    @Query("DELETE FROM bookmarks WHERE event_id IN (:eventIds)")
    abstract suspend fun removeBookmarks(eventIds: LongArray): Int
}