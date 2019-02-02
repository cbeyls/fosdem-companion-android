package be.digitalia.fosdem.db;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import be.digitalia.fosdem.alarms.FosdemAlarmManager;
import be.digitalia.fosdem.db.entities.Bookmark;
import be.digitalia.fosdem.model.AlarmInfo;
import be.digitalia.fosdem.model.Event;

import java.util.List;

@Dao
public abstract class BookmarksDao {

	/**
	 * Returns the bookmarks.
	 *
	 * @param minStartTime When greater than 0, only return the events starting after this time.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ " FROM bookmarks b"
			+ " JOIN events e ON b.event_id = e.id"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE e.start_time > :minStartTime"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract LiveData<List<Event>> getBookmarks(long minStartTime);

	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ " FROM bookmarks b"
			+ " JOIN events e ON b.event_id = e.id"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	@WorkerThread
	public abstract Event[] getBookmarks();

	@Query("SELECT b.event_id, e.start_time"
			+ " FROM bookmarks b"
			+ " JOIN events e ON b.event_id = e.id"
			+ " WHERE e.start_time > :minStartTime"
			+ " ORDER BY e.start_time ASC")
	@WorkerThread
	public abstract AlarmInfo[] getBookmarksAlarmInfo(long minStartTime);

	@Query("SELECT COUNT(*) FROM bookmarks WHERE event_id = :event")
	public abstract LiveData<Boolean> getBookmarkStatus(Event event);

	public void addBookmark(@NonNull Event event) {
		if (addBookmarkInternal(new Bookmark(event.getId())) != -1L) {
			FosdemAlarmManager.getInstance().onBookmarkAdded(event);
		}
	}

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract long addBookmarkInternal(Bookmark bookmark);

	@Delete
	public void removeBookmark(@NonNull Event event) {
		removeBookmarks(event.getId());
	}

	public void removeBookmarks(@NonNull long... eventIds) {
		if (removeBookmarksInternal(eventIds) > 0) {
			FosdemAlarmManager.getInstance().onBookmarksRemoved(eventIds);
		}
	}

	@Query("DELETE FROM bookmarks WHERE event_id IN (:eventIds)")
	protected abstract int removeBookmarksInternal(long[] eventIds);
}
