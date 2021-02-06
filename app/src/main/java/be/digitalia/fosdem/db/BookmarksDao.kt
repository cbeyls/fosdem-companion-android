package be.digitalia.fosdem.db

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.withTransaction
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.db.entities.Bookmark
import be.digitalia.fosdem.model.AlarmInfo
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.BackgroundWorkScope
import kotlinx.coroutines.launch

@Dao
abstract class BookmarksDao(private val appDatabase: AppDatabase) {

    /**
     * Returns the bookmarks.
     *
     * @param minStartTime When greater than 0, only return the events starting after this time.
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
    abstract fun getBookmarks(minStartTime: Long): LiveData<List<Event>>

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
    abstract fun getBookmarks(): Array<Event>

    @Query("""SELECT b.event_id, e.start_time
        FROM bookmarks b
        JOIN events e ON b.event_id = e.id
        WHERE e.start_time > :minStartTime
        ORDER BY e.start_time ASC""")
    @WorkerThread
    abstract fun getBookmarksAlarmInfo(minStartTime: Long): Array<AlarmInfo>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE event_id = :event")
    abstract fun getBookmarkStatus(event: Event): LiveData<Boolean>

    fun addBookmarkAsync(event: Event) {
        BackgroundWorkScope.launch {
            val ids = addBookmarksInternal(listOf(Bookmark(event.id)))
            if (ids[0] != -1L) {
                FosdemAlarmManager.onBookmarksAdded(listOf(AlarmInfo(eventId = event.id, startTime = event.startTime)))
            }
        }
    }

    fun addBookmarksAsync(eventIds: LongArray) {
        BackgroundWorkScope.launch {
            appDatabase.withTransaction {
                // Get AlarmInfos first to filter out non-existing items
                val alarmInfos = getAlarmInfos(eventIds)
                alarmInfos.isNotEmpty() || return@withTransaction

                val ids = addBookmarksInternal(alarmInfos.map { Bookmark(it.eventId) })
                // Filter out items that were already in bookmarks
                val addedAlarmInfos = alarmInfos.filterIndexed { index, _ -> ids[index] != -1L }
                if (addedAlarmInfos.isNotEmpty()) {
                    FosdemAlarmManager.onBookmarksAdded(addedAlarmInfos)
                }
            }
        }
    }

    @Query("""SELECT id as event_id, start_time
        FROM events
        WHERE id IN (:ids)
        ORDER BY start_time ASC""")
    protected abstract suspend fun getAlarmInfos(ids: LongArray): List<AlarmInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addBookmarksInternal(bookmarks: List<Bookmark>): LongArray

    fun removeBookmarkAsync(event: Event) {
        removeBookmarksAsync(longArrayOf(event.id))
    }

    fun removeBookmarksAsync(eventIds: LongArray) {
        BackgroundWorkScope.launch {
            if (removeBookmarksInternal(eventIds) > 0) {
                FosdemAlarmManager.onBookmarksRemoved(eventIds)
            }
        }
    }

    @Query("DELETE FROM bookmarks WHERE event_id IN (:eventIds)")
    protected abstract suspend fun removeBookmarksInternal(eventIds: LongArray): Int
}