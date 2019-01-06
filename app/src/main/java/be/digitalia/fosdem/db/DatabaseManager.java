package be.digitalia.fosdem.db;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;
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
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;

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

	private static final String DB_PREFS_FILE = "database";
	private static final String LAST_UPDATE_TIME_PREF = "last_update_time";
	private static final String LAST_MODIFIED_TAG_PREF = "last_modified_tag";

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

	private static final String TRACK_INSERT_STATEMENT = "INSERT INTO " + Track.TABLE_NAME + " (id, name, type) VALUES (?, ?, ?);";
	private static final String EVENT_INSERT_STATEMENT = "INSERT INTO " + EventEntity.TABLE_NAME
			+ " (id, day_index, start_time, end_time, room_name, slug, track_id, abstract, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String EVENT_TITLES_INSERT_STATEMENT = "INSERT INTO " + EventTitles.TABLE_NAME
			+ " (`rowid`, title, subtitle) VALUES (?, ?, ?);";
	private static final String EVENT_PERSON_INSERT_STATEMENT = "INSERT INTO " + EventToPerson.TABLE_NAME
			+ " (event_id, person_id) VALUES (?, ?);";
	// Ignore conflicts in case of existing person
	private static final String PERSON_INSERT_STATEMENT = "INSERT OR IGNORE INTO " + Person.TABLE_NAME + " (`rowid`, name) VALUES (?, ?);";
	private static final String LINK_INSERT_STATEMENT = "INSERT INTO " + Link.TABLE_NAME + " (event_id, url, description) VALUES (?, ?, ?);";

	private static void bindString(SupportSQLiteStatement statement, int index, String value) {
		if (value == null) {
			statement.bindNull(index);
		} else {
			statement.bindString(index, value);
		}
	}

	private SharedPreferences getSharedPreferences() {
		return context.getSharedPreferences(DB_PREFS_FILE, Context.MODE_PRIVATE);
	}

	/**
	 * @return The last update time in milliseconds since EPOCH, or -1 if not available.
	 */
	public long getLastUpdateTime() {
		return getSharedPreferences().getLong(LAST_UPDATE_TIME_PREF, -1L);
	}

	/**
	 * @return The time identifier of the current version of the database.
	 */
	public String getLastModifiedTag() {
		return getSharedPreferences().getString(LAST_MODIFIED_TAG_PREF, null);
	}

	/**
	 * Stores the schedule to the database.
	 *
	 * @param events
	 * @return The number of events processed.
	 */
	@WorkerThread
	public int storeSchedule(Iterable<DetailedEvent> events, String lastModifiedTag) {
		boolean isComplete = false;
		List<Day> daysList = null;

		SupportSQLiteDatabase db = helper.getWritableDatabase();
		appDatabase.beginTransaction();
		try {
			// 1: Delete the previous schedule
			appDatabase.getEventDao().clearSchedule();

			// Compile the insert statements for the big tables
			final SupportSQLiteStatement trackInsertStatement = db.compileStatement(TRACK_INSERT_STATEMENT);
			final SupportSQLiteStatement eventInsertStatement = db.compileStatement(EVENT_INSERT_STATEMENT);
			final SupportSQLiteStatement eventTitlesInsertStatement = db.compileStatement(EVENT_TITLES_INSERT_STATEMENT);
			final SupportSQLiteStatement eventPersonInsertStatement = db.compileStatement(EVENT_PERSON_INSERT_STATEMENT);
			final SupportSQLiteStatement personInsertStatement = db.compileStatement(PERSON_INSERT_STATEMENT);
			final SupportSQLiteStatement linkInsertStatement = db.compileStatement(LINK_INSERT_STATEMENT);

			// 2: Insert the events
			int totalEvents = 0;
			Map<Track, Long> tracks = new HashMap<>();
			long nextTrackId = 0L;
			long minEventId = Long.MAX_VALUE;
			Set<Day> days = new HashSet<>(2);

			for (DetailedEvent event : events) {
				// 2a: Retrieve or insert Track
				Track track = event.getTrack();
				Long trackId = tracks.get(track);
				if (trackId == null) {
					// New track
					nextTrackId++;
					trackId = nextTrackId;
					trackInsertStatement.clearBindings();
					trackInsertStatement.bindLong(1, nextTrackId);
					bindString(trackInsertStatement, 2, track.getName());
					bindString(trackInsertStatement, 3, track.getType().name());
					if (trackInsertStatement.executeInsert() != -1L) {
						tracks.put(track, trackId);
					}
				}

				// 2b: Insert main event
				eventInsertStatement.clearBindings();
				long eventId = event.getId();
				if (eventId < minEventId) {
					minEventId = eventId;
				}
				eventInsertStatement.bindLong(1, eventId);
				Day day = event.getDay();
				days.add(day);
				eventInsertStatement.bindLong(2, day.getIndex());
				Date time = event.getStartTime();
				if (time == null) {
					eventInsertStatement.bindNull(3);
				} else {
					eventInsertStatement.bindLong(3, time.getTime());
				}
				time = event.getEndTime();
				if (time == null) {
					eventInsertStatement.bindNull(4);
				} else {
					eventInsertStatement.bindLong(4, time.getTime());
				}
				bindString(eventInsertStatement, 5, event.getRoomName());
				bindString(eventInsertStatement, 6, event.getSlug());
				eventInsertStatement.bindLong(7, trackId);
				bindString(eventInsertStatement, 8, event.getAbstractText());
				bindString(eventInsertStatement, 9, event.getDescription());

				if (eventInsertStatement.executeInsert() != -1L) {
					// 2c: Insert fulltext fields
					eventTitlesInsertStatement.clearBindings();
					eventTitlesInsertStatement.bindLong(1, eventId);
					bindString(eventTitlesInsertStatement, 2, event.getTitle());
					bindString(eventTitlesInsertStatement, 3, event.getSubTitle());
					eventTitlesInsertStatement.executeInsert();

					// 2d: Insert persons
					for (Person person : event.getPersons()) {
						eventPersonInsertStatement.clearBindings();
						eventPersonInsertStatement.bindLong(1, eventId);
						long personId = person.getId();
						eventPersonInsertStatement.bindLong(2, personId);
						eventPersonInsertStatement.executeInsert();

						personInsertStatement.clearBindings();
						personInsertStatement.bindLong(1, personId);
						bindString(personInsertStatement, 2, person.getName());
						try {
							personInsertStatement.executeInsert();
						} catch (SQLiteConstraintException e) {
							// Older Android versions may not ignore an existing person
						}
					}

					// 2e: Insert links
					for (Link link : event.getLinks()) {
						linkInsertStatement.clearBindings();
						linkInsertStatement.bindLong(1, eventId);
						bindString(linkInsertStatement, 2, link.getUrl());
						bindString(linkInsertStatement, 3, link.getDescription());
						linkInsertStatement.executeInsert();
					}
				}

				totalEvents++;
			}

			// 3: Insert collected days
			ContentValues values = new ContentValues();
			for (Day day : days) {
				values.clear();
				values.put("`index`", day.getIndex());
				values.put("date", day.getDate().getTime());
				db.insert(Day.TABLE_NAME, SQLiteDatabase.CONFLICT_ABORT, values);
			}
			daysList = new ArrayList<>(days);
			Collections.sort(daysList);

			// 4: Purge outdated bookmarks
			if (minEventId < Long.MAX_VALUE) {
				String[] whereArgs = new String[]{String.valueOf(minEventId)};
				db.delete(Bookmark.TABLE_NAME, "event_id < ?", whereArgs);
			}

			if (totalEvents > 0) {
				appDatabase.setTransactionSuccessful();
				isComplete = true;
			}

			return totalEvents;
		} finally {
			appDatabase.endTransaction();

			if (isComplete) {
				// Set last update time and server's last modified tag
				getSharedPreferences().edit()
						.putLong(LAST_UPDATE_TIME_PREF, System.currentTimeMillis())
						.putString(LAST_MODIFIED_TAG_PREF, lastModifiedTag)
						.apply();

				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_SCHEDULE_REFRESHED));
			}
		}
	}

	@WorkerThread
	public Cursor getTracks(Day day) {
		String[] selectionArgs = new String[]{String.valueOf(day.getIndex())};
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT t.id AS _id, t.name, t.type" + " FROM " + Track.TABLE_NAME + " t"
						+ " JOIN " + EventEntity.TABLE_NAME + " e ON t.id = e.track_id"
						+ " WHERE e.day_index = ?"
						+ " GROUP BY t.id"
						+ " ORDER BY t.name ASC", selectionArgs);
		return new LocalBroadcastCursor(cursor, context, new IntentFilter(ACTION_SCHEDULE_REFRESHED));
	}

	public static Track toTrack(Cursor cursor, Track track) {
		if (track == null) {
			track = new Track();
		}
		track.setName(cursor.getString(1));
		track.setType(Enum.valueOf(Track.Type.class, cursor.getString(2)));

		return track;
	}

	public static Track toTrack(Cursor cursor) {
		return toTrack(cursor, null);
	}

	/**
	 * Returns the event with the specified id, or null if not found.
	 */
	@WorkerThread
	@Nullable
	public Event getEvent(long id) {
		String[] selectionArgs = new String[]{String.valueOf(id)};
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " WHERE e.id = ?"
						+ " GROUP BY e.id", selectionArgs);
		try {
			if (cursor.moveToFirst()) {
				return toEvent(cursor);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	private Cursor toEventCursor(Cursor wrappedCursor) {
		IntentFilter intentFilter = new IntentFilter(ACTION_SCHEDULE_REFRESHED);
		intentFilter.addAction(ACTION_ADD_BOOKMARK);
		intentFilter.addAction(ACTION_REMOVE_BOOKMARKS);
		return new LocalBroadcastCursor(wrappedCursor, context, intentFilter);
	}

	/**
	 * Returns the events for a specified track.
	 *
	 * @param day
	 * @param track
	 * @return A cursor to Events
	 */
	@WorkerThread
	public Cursor getEvents(Day day, Track track) {
		String[] selectionArgs = new String[]{String.valueOf(day.getIndex()), track.getName(), track.getType().name()};
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
						+ " FROM " + EventEntity.TABLE_NAME + " e"
						+ " JOIN " + EventTitles.TABLE_NAME + " et ON e.id = et.rowid"
						+ " JOIN " + Day.TABLE_NAME + " d ON e.day_index = d.`index`"
						+ " JOIN " + Track.TABLE_NAME + " t ON e.track_id = t.id"
						+ " LEFT JOIN " + EventToPerson.TABLE_NAME + " ep ON e.id = ep.event_id"
						+ " LEFT JOIN " + Person.TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " LEFT JOIN " + Bookmark.TABLE_NAME + " b ON e.id = b.event_id"
						+ " WHERE e.day_index = ? AND t.name = ? AND t.type = ?"
						+ " GROUP BY e.id"
						+ " ORDER BY e.start_time ASC", selectionArgs);
		return toEventCursor(cursor);
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

	/**
	 * Returns all persons in alphabetical order.
	 */
	@WorkerThread
	public Cursor getPersons() {
		Cursor cursor = helper.getReadableDatabase().query(
				"SELECT rowid AS _id, name"
						+ " FROM " + Person.TABLE_NAME
						+ " ORDER BY name COLLATE NOCASE", null);
		return new LocalBroadcastCursor(cursor, context, new IntentFilter(ACTION_SCHEDULE_REFRESHED));
	}

	public static Person toPerson(Cursor cursor, Person person) {
		if (person == null) {
			person = new Person();
		}
		person.setId(cursor.getLong(0));
		person.setName(cursor.getString(1));

		return person;
	}

	public static Person toPerson(Cursor cursor) {
		return toPerson(cursor, null);
	}

	@WorkerThread
	public boolean isBookmarked(Event event) {
		String[] selectionArgs = new String[]{String.valueOf(event.getId())};
		return queryNumEntries(helper.getReadableDatabase(), Bookmark.TABLE_NAME, "event_id = ?", selectionArgs) > 0L;
	}

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
