package be.digitalia.fosdem.db

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
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class BookmarksDao(appDatabase: AppDatabase) {
    val version: Flow<Int> =
        appDatabase.createVersionFlow(EventEntity.TABLE_NAME, Bookmark.TABLE_NAME)

    /**
     * Returns the bookmarks.
     *
     * @param minEndTime Only return the events ending after this time.
     */
    @Query("""SELECT ev.*
        FROM bookmarks b
        JOIN events_view ev ON b.event_id = ev.id
        WHERE ev.end_time > :minEndTime
        ORDER BY ev.start_time ASC""")
    @TypeConverters(NonNullInstantTypeConverters::class)
    abstract suspend fun getBookmarks(minEndTime: Instant = Instant.EPOCH): List<Event>

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