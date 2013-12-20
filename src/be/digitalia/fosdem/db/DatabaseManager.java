package be.digitalia.fosdem.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;

/**
 * Here comes the badass SQL.
 * 
 * @author Christophe Beyls
 * 
 */
public class DatabaseManager {

	private static final Uri URI_SCHEDULE = Uri.parse("sqlite://be.digitalia.fosdem/schedule");
	private static final Uri URI_BOOKMARKS = Uri.parse("sqlite://be.digitalia.fosdem/bookmarks");

	private static final String DB_PREFS_FILE = "database";
	private static final String LAST_UPDATE_TIME_PREF = "last_update_time";

	private static DatabaseManager instance;

	private Context context;
	private DatabaseHelper helper;

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
		helper = new DatabaseHelper(context);
	}

	private static final String[] countProjection = new String[] { "count(*)" };

	private static long queryNumEntries(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
		Cursor cursor = db.query(table, countProjection, selection, selectionArgs, null, null, null);
		try {
			cursor.moveToFirst();
			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	private static final String TRACK_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.TRACKS_TABLE_NAME + " (id, name, type) VALUES (?, ?, ?);";
	private static final String EVENT_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_TABLE_NAME
			+ " (id, day_index, start_time, end_time, room_name, slug, track_id, abstract, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String EVENT_TITLES_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME
			+ " (rowid, title, subtitle) VALUES (?, ?, ?);";
	private static final String EVENT_PERSON_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
			+ " (event_id, person_id) VALUES (?, ?);";
	// Ignore conflicts in case of existing person
	private static final String PERSON_INSERT_STATEMENT = "INSERT OR IGNORE INTO " + DatabaseHelper.PERSONS_TABLE_NAME + " (rowid, name) VALUES (?, ?);";
	private static final String LINK_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.LINKS_TABLE_NAME + " (event_id, url, description) VALUES (?, ?, ?);";

	private static void bindString(SQLiteStatement statement, int index, String value) {
		if (value == null) {
			statement.bindNull(index);
		} else {
			statement.bindString(index, value);
		}
	}

	private SharedPreferences getSharedPreferences() {
		return context.getSharedPreferences(DB_PREFS_FILE, Context.MODE_PRIVATE);
	}

	public Date getLastUpdateTime() {
		long time = getSharedPreferences().getLong(LAST_UPDATE_TIME_PREF, -1L);
		return (time == -1L) ? null : new Date(time);
	}

	/**
	 * Stores the schedule to the database.
	 * 
	 * @param events
	 * @return The number of events processed.
	 */
	public int storeSchedule(Iterable<Event> events) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			// 1: Delete the previous schedule
			clearSchedule(db);

			// Compile the insert statements for the big tables
			final SQLiteStatement trackInsertStatement = db.compileStatement(TRACK_INSERT_STATEMENT);
			final SQLiteStatement eventInsertStatement = db.compileStatement(EVENT_INSERT_STATEMENT);
			final SQLiteStatement eventTitlesInsertStatement = db.compileStatement(EVENT_TITLES_INSERT_STATEMENT);
			final SQLiteStatement eventPersonInsertStatement = db.compileStatement(EVENT_PERSON_INSERT_STATEMENT);
			final SQLiteStatement personInsertStatement = db.compileStatement(PERSON_INSERT_STATEMENT);
			final SQLiteStatement linkInsertStatement = db.compileStatement(LINK_INSERT_STATEMENT);

			// 2: Insert the events
			int totalEvents = 0;
			Map<Track, Long> tracks = new HashMap<Track, Long>();
			long nextTrackId = 0L;
			Set<Day> days = new HashSet<Day>(2);

			for (Event event : events) {
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
				values.put("_index", day.getIndex());
				Date date = day.getDate();
				values.put("date", (date == null) ? 0L : date.getTime());
				db.insert(DatabaseHelper.DAYS_TABLE_NAME, null, values);
			}

			// TODO purge outdated bookmarks ?

			db.setTransactionSuccessful();

			// Set last update time
			getSharedPreferences().edit().putLong(LAST_UPDATE_TIME_PREF, System.currentTimeMillis()).commit();

			return totalEvents;
		} finally {
			db.endTransaction();
			context.getContentResolver().notifyChange(URI_SCHEDULE, null);
			context.getContentResolver().notifyChange(URI_BOOKMARKS, null);
		}
	}

	public void clearSchedule() {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			clearSchedule(db);

			db.setTransactionSuccessful();

			getSharedPreferences().edit().remove(LAST_UPDATE_TIME_PREF).commit();
		} finally {
			db.endTransaction();
		}
	}

	private static void clearSchedule(SQLiteDatabase db) {
		db.delete(DatabaseHelper.EVENTS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.EVENTS_TITLES_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.PERSONS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.EVENTS_PERSONS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.LINKS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.TRACKS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.DAYS_TABLE_NAME, null, null);
	}

	/**
	 * 
	 * @return The Days the events span to.
	 */
	public List<Day> getDays() {
		Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT _index, date" + " FROM " + DatabaseHelper.DAYS_TABLE_NAME + " ORDER BY _index ASC", null);
		try {
			List<Day> result = new ArrayList<Day>(cursor.getCount());
			while (cursor.moveToNext()) {
				Day day = new Day();
				day.setIndex(cursor.getInt(0));
				day.setDate(new Date(cursor.getLong(1)));
				result.add(day);
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	public Cursor getTracks(Day day) {
		String[] selectionArgs = new String[] { String.valueOf(day.getIndex()) };
		Cursor cursor = helper.getReadableDatabase().rawQuery(
				"SELECT t.id AS _id, t.name, t.type" + " FROM " + DatabaseHelper.TRACKS_TABLE_NAME + " t" + " JOIN " + DatabaseHelper.EVENTS_TABLE_NAME
						+ " e ON t.id = e.track_id" + " WHERE e.day_index = ?" + " GROUP BY t.id" + " ORDER BY t.id ASC", selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
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

	public long getEventsCount() {
		return queryNumEntries(helper.getReadableDatabase(), DatabaseHelper.EVENTS_TABLE_NAME, null, null);
	}

	/**
	 * 
	 * @param day
	 * @param track
	 * @return A cursor to Events
	 */
	public Cursor getEvents(Day day, Track track) {
		String[] selectionArgs = new String[] { String.valueOf(day.getIndex()), track.getName(), track.getType().name() };
		Cursor cursor = helper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " JOIN "
								+ DatabaseHelper.PERSONS_TABLE_NAME
								+ " p ON ep.person_id = p.rowid"
								+ " WHERE e.day_index = ? AND t.name = ? AND t.type = ?" + " GROUP BY e.id" + " ORDER BY e.start_time ASC", selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
	}

	/**
	 * Returns the events presented by the specified person.
	 * 
	 * @param person
	 * @return A cursor to Events
	 */
	public Cursor getEvents(Person person) {
		String[] selectionArgs = new String[] { String.valueOf(person.getId()) };
		Cursor cursor = helper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " JOIN "
								+ DatabaseHelper.PERSONS_TABLE_NAME
								+ " p ON ep.person_id = p.rowid"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep2 ON e.id = ep2.event_id" + " WHERE ep2.person_id = ?" + " GROUP BY e.id" + " ORDER BY e.start_time ASC", selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
	}

	/**
	 * Returns the bookmarks.
	 * 
	 * @param futureOnly
	 *            When true, only return the events that are not over yet.
	 * @return A cursor to Events
	 */
	public Cursor getBookmarks(boolean futureOnly) {
		String whereCondition;
		String[] selectionArgs;
		if (futureOnly) {
			whereCondition = " WHERE e.end_time > ?";
			selectionArgs = new String[] { String.valueOf(System.currentTimeMillis()) };
		} else {
			whereCondition = "";
			selectionArgs = null;
		}

		Cursor cursor = helper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
								+ " FROM "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e ON b.event_id = e.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " JOIN "
								+ DatabaseHelper.PERSONS_TABLE_NAME
								+ " p ON ep.person_id = p.rowid" + whereCondition + " GROUP BY e.id" + " ORDER BY e.start_time ASC", selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_BOOKMARKS);
		return cursor;
	}

	/**
	 * Search through matching titles, subtitles, track names, person names. We need to use an union of 3 sub-queries because a "match" condition can not be
	 * accompanied by other conditions in a "where" statement.
	 * 
	 * @param query
	 * @return A cursor to Events
	 */
	public Cursor getSearchResults(String query) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[] { matchQuery, "%" + query + "%", matchQuery };
		Cursor cursor = helper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " JOIN "
								+ DatabaseHelper.PERSONS_TABLE_NAME
								+ " p ON ep.person_id = p.rowid"
								+ " WHERE e.id IN ( "
								+ "SELECT rowid"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " WHERE "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " MATCH ?"
								+ " UNION "
								+ "SELECT e.id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " WHERE t.name LIKE ?"
								+ " UNION "
								+ "SELECT ep.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
								+ " ep"
								+ " JOIN "
								+ DatabaseHelper.PERSONS_TABLE_NAME
								+ " p ON ep.person_id = p.rowid" + " WHERE p.name MATCH ?" + " )" + " GROUP BY e.id" + " ORDER BY e.start_time ASC",
						selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
	}

	/**
	 * Method called by SearchSuggestionProvider to return search results in the format expected by the search framework.
	 * 
	 */
	public Cursor getSearchSuggestionResults(String query) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[] { matchQuery, "%" + query + "%", matchQuery };
		// Query is similar to getSearchResults but returns different columns, does not join the Day table and limits the result set to 5 entries.
		Cursor cursor = helper.getReadableDatabase().rawQuery(
				"SELECT e.id AS " + BaseColumns._ID + ", et.title AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
						+ ", GROUP_CONCAT(p.name, ', ') || ' - ' || t.name AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + " FROM "
						+ DatabaseHelper.EVENTS_TABLE_NAME + " e" + " JOIN " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME + " et ON e.id = et.rowid" + " JOIN "
						+ DatabaseHelper.TRACKS_TABLE_NAME + " t ON e.track_id = t.id" + " JOIN " + DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
						+ " ep ON e.id = ep.event_id" + " JOIN " + DatabaseHelper.PERSONS_TABLE_NAME + " p ON ep.person_id = p.rowid" + " WHERE e.id IN ( "
						+ "SELECT rowid" + " FROM " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME + " WHERE " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME
						+ " MATCH ?" + " UNION " + "SELECT e.id" + " FROM " + DatabaseHelper.EVENTS_TABLE_NAME + " e" + " JOIN "
						+ DatabaseHelper.TRACKS_TABLE_NAME + " t ON e.track_id = t.id" + " WHERE t.name LIKE ?" + " UNION " + "SELECT ep.event_id" + " FROM "
						+ DatabaseHelper.EVENTS_PERSONS_TABLE_NAME + " ep" + " JOIN " + DatabaseHelper.PERSONS_TABLE_NAME + " p ON ep.person_id = p.rowid"
						+ " WHERE p.name MATCH ?" + " )" + " GROUP BY e.id" + " ORDER BY e.start_time ASC LIMIT 5", selectionArgs);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
	}

	public static Event toEvent(Cursor cursor, Event event) {
		Day day;
		Track track;
		if (event == null) {
			event = new Event();
			day = new Day();
			event.setDay(day);
			track = new Track();
			event.setTrack(track);

			event.setStartTime(new Date(cursor.getLong(1)));
			event.setEndTime(new Date(cursor.getLong(2)));

			day.setDate(new Date(cursor.getLong(11)));
		} else {
			day = event.getDay();
			track = event.getTrack();

			event.getStartTime().setTime(cursor.getLong(1));
			event.getEndTime().setTime(cursor.getLong(2));

			day.getDate().setTime(cursor.getLong(11));
		}
		event.setId(cursor.getInt(0));
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

	/**
	 * Returns all persons in alphabetical order.
	 */
	public Cursor getPersons() {
		Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT rowid AS _id, name" + " FROM " + DatabaseHelper.PERSONS_TABLE_NAME + " ORDER BY name ASC", null);
		cursor.setNotificationUri(context.getContentResolver(), URI_SCHEDULE);
		return cursor;
	}

	/**
	 * Returns persons presenting the specified event.
	 */
	public List<Person> getPersons(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		Cursor cursor = helper.getReadableDatabase().rawQuery(
				"SELECT p.rowid AS _id, p.name" + " FROM " + DatabaseHelper.PERSONS_TABLE_NAME + " p" + " JOIN " + DatabaseHelper.EVENTS_PERSONS_TABLE_NAME
						+ " ep ON p.rowid = ep.person_id" + " WHERE ep.event_id = ?" + " ORDER BY p.name ASC", selectionArgs);
		try {
			List<Person> result = new ArrayList<Person>(cursor.getCount());
			while (cursor.moveToNext()) {
				result.add(toPerson(cursor));
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	public static Person toPerson(Cursor cursor, Person person) {
		if (person == null) {
			person = new Person();
		}
		person.setId(cursor.getInt(0));
		person.setName(cursor.getString(1));

		return person;
	}

	public static Person toPerson(Cursor cursor) {
		return toPerson(cursor, null);
	}

	public List<Link> getLinks(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		Cursor cursor = helper.getReadableDatabase().rawQuery(
				"SELECT url, description" + " FROM " + DatabaseHelper.LINKS_TABLE_NAME + " WHERE event_id = ?" + " ORDER BY rowid ASC", selectionArgs);
		try {
			List<Link> result = new ArrayList<Link>(cursor.getCount());
			while (cursor.moveToNext()) {
				Link link = new Link();
				link.setUrl(cursor.getString(0));
				link.setDescription(cursor.getString(1));
				result.add(link);
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	public boolean isBookmarked(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		return queryNumEntries(helper.getReadableDatabase(), DatabaseHelper.BOOKMARKS_TABLE_NAME, "event_id = ?", selectionArgs) > 0L;
	}

	public boolean addBookmark(Event event) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("event_id", event.getId());
			long result = db.insert(DatabaseHelper.BOOKMARKS_TABLE_NAME, null, values);

			// If the bookmark is already present
			if (result == -1L) {
				return false;
			}

			db.setTransactionSuccessful();
			return true;
		} finally {
			db.endTransaction();
			context.getContentResolver().notifyChange(URI_BOOKMARKS, null);
		}
	}

	public boolean removeBookmark(Event event) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			String[] whereArgs = new String[] { String.valueOf(event.getId()) };
			int count = db.delete(DatabaseHelper.BOOKMARKS_TABLE_NAME, "event_id = ?", whereArgs);

			if (count == 0) {
				return false;
			}

			db.setTransactionSuccessful();
			return true;
		} finally {
			db.endTransaction();
			context.getContentResolver().notifyChange(URI_BOOKMARKS, null);
		}
	}
}
