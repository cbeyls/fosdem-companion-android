package be.digitalia.fosdem.db;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteStatement;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.db.entities.Bookmark;
import be.digitalia.fosdem.db.entities.EventEntity;
import be.digitalia.fosdem.db.entities.EventTitles;
import be.digitalia.fosdem.db.entities.EventToPerson;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Here comes the badass SQL.
 *
 * @author Christophe Beyls
 */
public class DatabaseManager {

	public static final String ACTION_SCHEDULE_REFRESHED = BuildConfig.APPLICATION_ID + ".action.SCHEDULE_REFRESHED";
	public static final String ACTION_ADD_BOOKMARK = BuildConfig.APPLICATION_ID + ".action.ADD_BOOKMARK";
	public static final String EXTRA_EVENT_ID = "event_id";
	public static final String EXTRA_EVENT_START_TIME = "event_start";
	public static final String ACTION_REMOVE_BOOKMARKS = BuildConfig.APPLICATION_ID + ".action.REMOVE_BOOKMARKS";
	public static final String EXTRA_EVENT_IDS = "event_ids";

	private static DatabaseManager instance;

	private final Context context;
	private final AppDatabase appDatabase;
	private final SupportSQLiteOpenHelper helper;

	public static void init(Context context) {
		if (instance == null) {
			instance = new DatabaseManager(context);
		}
	}

	public static DatabaseManager getInstance() {
		return instance;
	}

	private DatabaseManager(Context context) {
		this.context = context;
		appDatabase = AppDatabase.getInstance(context);
		helper = appDatabase.getOpenHelper();
	}

	private Cursor toEventCursor(Cursor wrappedCursor) {
		IntentFilter intentFilter = new IntentFilter(ACTION_SCHEDULE_REFRESHED);
		intentFilter.addAction(ACTION_ADD_BOOKMARK);
		intentFilter.addAction(ACTION_REMOVE_BOOKMARKS);
		return new LocalBroadcastCursor(wrappedCursor, context, intentFilter);
	}

	/**
	 * Returns the events in the specified time window, ordered by start time. All parameters are optional but at least one must be provided.
	 *
	 * @param minStartTime Minimum start time, or -1
	 * @param maxStartTime Maximum start time, or -1
	 * @param minEndTime   Minimum end time, or -1
	 * @param ascending    If true, order results from start time ascending, else order from start time descending
	 * @return
	 */
	@WorkerThread
	public Cursor getEvents(long minStartTime, long maxStartTime, long minEndTime, boolean ascending) {
		ArrayList<String> selectionArgs = new ArrayList<>(3);
		StringBuilder whereCondition = new StringBuilder();

		if (minStartTime > 0L) {
			whereCondition.append("e.start_time > ?");
			selectionArgs.add(String.valueOf(minStartTime));
		}
		if (maxStartTime > 0L) {
			if (whereCondition.length() > 0) {
				whereCondition.append(" AND ");
			}
			whereCondition.append("e.start_time < ?");
			selectionArgs.add(String.valueOf(maxStartTime));
		}
		if (minEndTime > 0L) {
			if (whereCondition.length() > 0) {
				whereCondition.append(" AND ");
			}
			whereCondition.append("e.end_time > ?");
			selectionArgs.add(String.valueOf(minEndTime));
		}
		if (whereCondition.length() == 0) {
			throw new IllegalArgumentException("At least one filter must be provided");
		}
		String ascendingString = ascending ? "ASC" : "DESC";

		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " LEFT JOIN " + Bookmark.TABLE_NAME + " b ON e.id = b.event_id"
						+ " WHERE " + whereCondition.toString()
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time " + ascendingString,
				selectionArgs.toArray(new String[selectionArgs.size()]));
		return toEventCursor(cursor);
	}

	/**
	 * Returns the events presented by the specified person.
	 *
	 * @param person
	 * @return A cursor to Events
	 */
	@WorkerThread
	public Cursor getEvents(Person person) {
		String[] selectionArgs = new String[]{String.valueOf(person.getId())};
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " LEFT JOIN " + Bookmark.TABLE_NAME + " b ON e.id = b.event_id"
						+ " JOIN " + EventToPerson.TABLE_NAME + " ep2 ON e.id = ep2.event_id"
						+ " WHERE ep2.person_id = ?"
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time ASC", selectionArgs);
		return toEventCursor(cursor);
	}

	/**
	 * Returns the bookmarks.
	 *
	 * @param minStartTime When positive, only return the events starting after this time.
	 * @return A cursor to Events
	 */
	@WorkerThread
	public Cursor getBookmarks(long minStartTime) {
		String whereCondition;
		String[] selectionArgs;
		if (minStartTime > 0L) {
			whereCondition = " WHERE e.start_time > ?";
			selectionArgs = new String[]{String.valueOf(minStartTime)};
		} else {
			whereCondition = "";
			selectionArgs = null;
		}

		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, 1"
						+ " FROM " + Bookmark.TABLE_NAME + " b"
						+ " JOIN " + EventEntity.TABLE_NAME + " e ON b.event_id = e.id"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ whereCondition
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time ASC", selectionArgs);
		return toEventCursor(cursor);
	}

	/**
	 * Search through matching titles, subtitles, track names, person names. We need to use an union of 3 sub-queries because a "match" condition can not be
	 * accompanied by other conditions in a "where" statement.
	 *
	 * @param query
	 * @return A cursor to Events
	 */
	@WorkerThread
	public Cursor getSearchResults(String query) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[]{matchQuery, "%" + query + "%", matchQuery};
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " LEFT JOIN " + Bookmark.TABLE_NAME + " b ON e.id = b.event_id"
						+ " WHERE e.id IN ( "
						+ "SELECT rowid"
						+ " FROM " + EventTitles.TABLE_NAME
						+ " WHERE " + EventTitles.TABLE_NAME + " MATCH ?"
						+ " UNION "
						+ "SELECT e.id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " WHERE t.name LIKE ?"
						+ " UNION "
						+ "SELECT ep.event_id"
						+ " FROM " + EventToPerson.TABLE_NAME + " ep"
						+ " JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " WHERE p.name MATCH ?"
						+ " )"
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time ASC", selectionArgs);
		return toEventCursor(cursor);
	}

	/**
	 * Method called by SearchSuggestionProvider to return search results in the format expected by the search framework.
	 */
	@WorkerThread
	public Cursor getSearchSuggestionResults(String query, int limit) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[]{matchQuery, "%" + query + "%", matchQuery, String.valueOf(limit)};
		// Query is similar to getSearchResults but returns different columns, does not join the Day table or the Bookmark table and limits the result set.
		return helper.getReadableDatabase().query(
				"SELECT e.id AS " + BaseColumns._ID
						+ ", et.title AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
						+ ", IFNULL(GROUP_CONCAT(p.name, ', '), '') || ' - ' || t.name AS " + SearchManager.SUGGEST_COLUMN_TEXT_2
						+ ", e.id AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " WHERE e.id IN ( "
						+ "SELECT rowid"
						+ " FROM " + EventTitles.TABLE_NAME
						+ " WHERE " + EventTitles.TABLE_NAME + " MATCH ?"
						+ " UNION "
						+ "SELECT e.id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " WHERE t.name LIKE ?"
						+ " UNION "
						+ "SELECT ep.event_id"
						+ " FROM " + EventToPerson.TABLE_NAME + " ep"
						+ " JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " WHERE p.name MATCH ?"
						+ " )"
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time ASC LIMIT ?", selectionArgs);
	}

	public static Event toEvent(Cursor cursor, Event event) {
		Day day;
		Track track;
		Date startTime;
		Date endTime;
		if (event == null) {
			event = new Event();
			day = new Day();
			event.setDay(day);
			track = new Track();
			event.setTrack(track);

			startTime = null;
			endTime = null;

			day.setDate(new Date(cursor.getLong(11)));
		} else {
			day = event.getDay();
			track = event.getTrack();

			startTime = event.getStartTime();
			endTime = event.getEndTime();

			day.getDate().setTime(cursor.getLong(11));
		}
		event.setId(cursor.getLong(0));
		if (cursor.isNull(1)) {
			event.setStartTime(null);
		} else {
			if (startTime == null) {
				event.setStartTime(new Date(cursor.getLong(1)));
			} else {
				startTime.setTime(cursor.getLong(1));
			}
		}
		if (cursor.isNull(2)) {
			event.setEndTime(null);
		} else {
			if (endTime == null) {
				event.setEndTime(new Date(cursor.getLong(2)));
			} else {
				endTime.setTime(cursor.getLong(2));
			}
		}

		event.setRoomName(cursor.getString(3));
		event.setSlug(cursor.getString(4));
		event.setTitle(cursor.getString(5));
		event.setSubTitle(cursor.getString(6));
		event.setAbstractText(cursor.getString(7));
		event.setDescription(cursor.getString(8));
		event.setPersonsSummary(cursor.getString(9));

		day.setIndex(cursor.getInt(10));

		track.setName(cursor.getString(12));
		track.setType(Enum.valueOf(Track.Type.class, cursor.getString(13)));

		return event;
	}

	public static Event toEvent(Cursor cursor) {
		return toEvent(cursor, null);
	}

	public static long toEventId(Cursor cursor) {
		return cursor.getLong(0);
	}

	public static long toEventStartTimeMillis(Cursor cursor) {
		return cursor.isNull(1) ? -1L : cursor.getLong(1);
	}

	public static long toEventEndTimeMillis(Cursor cursor) {
		return cursor.isNull(2) ? -1L : cursor.getLong(2);
	}

	public static boolean toBookmarkStatus(Cursor cursor) {
		return !cursor.isNull(14);
	}

	@WorkerThread
	public boolean isBookmarked(Event event) {
		String[] selectionArgs = new String[]{String.valueOf(event.getId())};
		return queryNumEntries(helper.getReadableDatabase(), Bookmark.TABLE_NAME, "event_id = ?", selectionArgs) > 0L;
	}

	@SuppressLint("RestrictedApi")
	@WorkerThread
	public boolean addBookmark(Event event) {
		boolean complete = false;

		SupportSQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("event_id", event.getId());
			long result = db.insert(Bookmark.TABLE_NAME, SQLiteDatabase.CONFLICT_ABORT, values);

			// If the bookmark is already present
			if (result == -1L) {
				return false;
			}

			db.setTransactionSuccessful();
			complete = true;
			return true;
		} finally {
			db.endTransaction();

			if (complete) {
				appDatabase.getInvalidationTracker().notifyObserversByTableNames(Bookmark.TABLE_NAME);
				Intent intent = new Intent(ACTION_ADD_BOOKMARK).putExtra(EXTRA_EVENT_ID, event.getId());
				Date startTime = event.getStartTime();
				if (startTime != null) {
					intent.putExtra(EXTRA_EVENT_START_TIME, startTime.getTime());
				}
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			}
		}
	}

	@WorkerThread
	public boolean removeBookmark(Event event) {
		return removeBookmarks(new long[]{event.getId()});
	}

	@SuppressLint("RestrictedApi")
	@WorkerThread
	public boolean removeBookmarks(long[] eventIds) {
		int length = eventIds.length;
		if (length == 0) {
			throw new IllegalArgumentException("At least one bookmark id to remove must be passed");
		}
		String[] stringEventIds = new String[length];
		for (int i = 0; i < length; ++i) {
			stringEventIds[i] = String.valueOf(eventIds[i]);
		}

		boolean isComplete = false;

		SupportSQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			String whereClause = "event_id IN (" + TextUtils.join(",", stringEventIds) + ")";
			int count = db.delete(Bookmark.TABLE_NAME, whereClause, null);

			if (count == 0) {
				return false;
			}

			db.setTransactionSuccessful();
			isComplete = true;
			return true;
		} finally {
			db.endTransaction();

			if (isComplete) {
				appDatabase.getInvalidationTracker().notifyObserversByTableNames(Bookmark.TABLE_NAME);
				Intent intent = new Intent(ACTION_REMOVE_BOOKMARKS).putExtra(EXTRA_EVENT_IDS, eventIds);
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			}
		}
	}

	// From DatabaseUtils

	public static long longForQuery(SupportSQLiteDatabase db, String query, String[] selectionArgs) {
		SupportSQLiteStatement prog = db.compileStatement(query);
		try {
			return longForQuery(prog, selectionArgs);
		} finally {
			try {
				prog.close();
			} catch (IOException ignore) {
			}
		}
	}

	public static long longForQuery(SupportSQLiteStatement prog, String[] selectionArgs) {
		if (selectionArgs != null) {
			for (int i = selectionArgs.length; i != 0; i--) {
				prog.bindString(i, selectionArgs[i - 1]);
			}
		}
		return prog.simpleQueryForLong();
	}

	public static long queryNumEntries(SupportSQLiteDatabase db, String table, String selection, String[] selectionArgs) {
		String s = (!TextUtils.isEmpty(selection)) ? " where " + selection : "";
		return longForQuery(db, "select count(*) from " + table + s, selectionArgs);
	}
}
