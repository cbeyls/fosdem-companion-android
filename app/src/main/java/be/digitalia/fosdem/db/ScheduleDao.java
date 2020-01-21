package be.digitalia.fosdem.db;

import android.app.SearchManager;
import android.database.Cursor;
import android.provider.BaseColumns;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.digitalia.fosdem.alarms.FosdemAlarmManager;
import be.digitalia.fosdem.db.entities.EventEntity;
import be.digitalia.fosdem.db.entities.EventTitles;
import be.digitalia.fosdem.db.entities.EventToPerson;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.EventDetails;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

@Dao
public abstract class ScheduleDao {

	private static final String LAST_UPDATE_TIME_PREF = "last_update_time";
	private static final String LAST_MODIFIED_TAG_PREF = "last_modified_tag";

	private final AppDatabase appDatabase;
	private final MutableLiveData<Long> lastUpdateTime = new MutableLiveData<>();

	public ScheduleDao(AppDatabase appDatabase) {
		this.appDatabase = appDatabase;
	}

	/**
	 * @return The last update time in milliseconds since EPOCH, or -1 if not available.
	 * This LiveData is pre-initialized with the up-to-date value.
	 */
	@MainThread
	public LiveData<Long> getLastUpdateTime() {
		if (lastUpdateTime.getValue() == null) {
			lastUpdateTime.setValue(appDatabase.getSharedPreferences().getLong(LAST_UPDATE_TIME_PREF, -1L));
		}
		return lastUpdateTime;
	}

	/**
	 * @return The time identifier of the current version of the database.
	 */
	public String getLastModifiedTag() {
		return appDatabase.getSharedPreferences().getString(LAST_MODIFIED_TAG_PREF, null);
	}

	private static class EmptyScheduleException extends RuntimeException {
	}

	/**
	 * Stores the schedule in the database.
	 *
	 * @param events The events stream.
	 * @return The number of events processed.
	 */
	@WorkerThread
	public int storeSchedule(Iterable<DetailedEvent> events, String lastModifiedTag) {
		int totalEvents;
		try {
			totalEvents = storeScheduleInternal(events, lastModifiedTag);
		} catch (EmptyScheduleException ese) {
			totalEvents = 0;
		}
		if (totalEvents > 0) {
			// Set last update time and server's last modified tag
			final long now = System.currentTimeMillis();
			appDatabase.getSharedPreferences().edit()
					.putLong(LAST_UPDATE_TIME_PREF, now)
					.putString(LAST_MODIFIED_TAG_PREF, lastModifiedTag)
					.apply();
			lastUpdateTime.postValue(now);

			FosdemAlarmManager.INSTANCE.onScheduleRefreshed();
		}
		return totalEvents;
	}

	@Transaction
	protected int storeScheduleInternal(Iterable<DetailedEvent> events, String lastModifiedTag) {
		// 1: Delete the previous schedule
		clearSchedule();

		// 2: Insert the events
		int totalEvents = 0;
		final Map<Track, Long> tracks = new HashMap<>();
		long nextTrackId = 0L;
		long minEventId = Long.MAX_VALUE;
		final Set<Day> days = new HashSet<>(2);

		for (DetailedEvent detailedEvent : events) {
			final Event event = detailedEvent.getEvent();
			final EventDetails details = detailedEvent.getDetails();
			// Retrieve or insert Track
			final Track track = event.getTrack();
			Long trackId = tracks.get(track);
			if (trackId == null) {
				// New track
				nextTrackId++;
				trackId = nextTrackId;
				final Track newTrack = new Track(nextTrackId, track.getName(), track.getType());
				insertTrack(newTrack);
				tracks.put(newTrack, trackId);
			}

			final long eventId = event.getId();
			try {
				// Insert main event and fulltext fields
				final EventEntity eventEntity = new EventEntity(
						eventId,
						event.getDay().getIndex(),
						event.getStartTime(),
						event.getEndTime(),
						event.getRoomName(),
						event.getSlug(),
						trackId,
						event.getAbstractText(),
						event.getDescription()
				);
				final EventTitles eventTitles = new EventTitles(
						eventId,
						event.getTitle(),
						event.getSubTitle()
				);
				insertEvent(eventEntity, eventTitles);
			} catch (Exception e) {
				// Duplicate event: skip
				continue;
			}

			days.add(event.getDay());
			if (eventId < minEventId) {
				minEventId = eventId;
			}

			final List<Person> persons = details.getPersons();
			insertPersons(persons);
			final int personsCount = persons.size();
			final EventToPerson[] eventsToPersons = new EventToPerson[personsCount];
			for (int i = 0; i < personsCount; ++i) {
				eventsToPersons[i] = new EventToPerson(eventId, persons.get(i).getId());
			}
			insertEventsToPersons(eventsToPersons);

			insertLinks(details.getLinks());

			totalEvents++;
		}

		if (totalEvents == 0) {
			// Rollback the transaction
			throw new EmptyScheduleException();
		}

		// 3: Insert collected days
		insertDays(days);

		// 4: Purge outdated bookmarks
		purgeOutdatedBookmarks(minEventId);

		return totalEvents;
	}

	@Insert
	protected abstract void insertTrack(Track track);

	@Insert
	protected abstract void insertEvent(EventEntity eventEntity, EventTitles eventTitles);

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract void insertPersons(List<Person> persons);

	@Insert
	protected abstract void insertEventsToPersons(EventToPerson[] eventsToPersons);

	@Insert
	protected abstract void insertLinks(List<Link> links);

	@Insert
	protected abstract void insertDays(Set<Day> days);

	@Query("DELETE FROM bookmarks WHERE event_id < :minEventId")
	protected abstract void purgeOutdatedBookmarks(long minEventId);

	@WorkerThread
	@Transaction
	public void clearSchedule() {
		clearEvents();
		clearEventTitles();
		clearPersons();
		clearEventToPersons();
		clearLinks();
		clearTracks();
		clearDays();
	}

	@Query("DELETE FROM events")
	protected abstract void clearEvents();

	@Query("DELETE FROM events_titles")
	protected abstract void clearEventTitles();

	@Query("DELETE FROM persons")
	protected abstract void clearPersons();

	@Query("DELETE FROM events_persons")
	protected abstract void clearEventToPersons();

	@Query("DELETE FROM links")
	protected abstract void clearLinks();

	@Query("DELETE FROM tracks")
	protected abstract void clearTracks();

	@Query("DELETE FROM days")
	protected abstract void clearDays();


	// Cache days
	private volatile LiveData<List<Day>> daysLiveData;

	public LiveData<List<Day>> getDays() {
		if (daysLiveData != null) {
			return daysLiveData;
		}
		synchronized (this) {
			daysLiveData = getDaysInternal();
			return daysLiveData;
		}
	}

	@Query("SELECT `index`, date FROM days ORDER BY `index` ASC")
	protected abstract LiveData<List<Day>> getDaysInternal();

	@WorkerThread
	public int getYear() {
		long date = 0L;

		// Compute from cached days if available
		final LiveData<List<Day>> cache = daysLiveData;
		List<Day> days = (cache == null) ? null : cache.getValue();
		if (days != null) {
			if (days.size() > 0) {
				date = days.get(0).getDate().getTime();
			}
		} else {
			date = getConferenceStartDate();
		}

		// Use the current year by default
		if (date == 0L) {
			date = System.currentTimeMillis();
		}

		return DateUtils.INSTANCE.getYear(date);
	}

	@Query("SELECT date FROM days ORDER BY `index` ASC LIMIT 1")
	protected abstract long getConferenceStartDate();

	@Query("SELECT t.id, t.name, t.type FROM tracks t"
			+ " JOIN events e ON t.id = e.track_id"
			+ " WHERE e.day_index = :day"
			+ " GROUP BY t.id"
			+ " ORDER BY t.name ASC")
	public abstract LiveData<List<Track>> getTracks(Day day);

	/**
	 * Returns the event with the specified id, or null if not found.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE e.id = :id"
			+ " GROUP BY e.id")
	@Nullable
	@WorkerThread
	public abstract Event getEvent(long id);

	/**
	 * Returns all found events whose id is part of the given list.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " WHERE e.id IN (:ids)"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract DataSource.Factory<Integer, StatusEvent> getEvents(long[] ids);

	/**
	 * Returns the events for a specified track.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " WHERE e.day_index = :day AND e.track_id = :track"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract LiveData<List<StatusEvent>> getEvents(Day day, Track track);

	/**
	 * Returns a snapshot of the events for a specified track (without the bookmark status).
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE e.day_index = :day AND e.track_id = :track"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract List<Event> getEventsSnapshot(Day day, Track track);

	/**
	 * Returns events starting in the specified interval, ordered by ascending start time.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " WHERE e.start_time BETWEEN :minStartTime AND :maxStartTime"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract DataSource.Factory<Integer, StatusEvent> getEventsWithStartTime(long minStartTime, long maxStartTime);

	/**
	 * Returns events in progress at the specified time, ordered by descending start time.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " WHERE e.start_time <= :time AND :time < e.end_time"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time DESC")
	public abstract DataSource.Factory<Integer, StatusEvent> getEventsInProgress(long time);

	/**
	 * Returns the events presented by the specified person.
	 */
	@Query("SELECT e.id , e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " JOIN events_persons ep2 ON e.id = ep2.event_id"
			+ " WHERE ep2.person_id = :person"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract DataSource.Factory<Integer, StatusEvent> getEvents(Person person);

	/**
	 * Search through matching titles, subtitles, track names, person names. We need to use an union of 3 sub-queries because a "match" condition can not be
	 * accompanied by other conditions in a "where" statement.
	 */
	@Query("SELECT e.id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description"
			+ ", GROUP_CONCAT(p.name, ', ') AS persons, e.day_index, d.date AS day_date, e.track_id, t.name AS track_name, t.type AS track_type"
			+ ", b.event_id IS NOT NULL AS is_bookmarked"
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN days d ON e.day_index = d.`index`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " LEFT JOIN bookmarks b ON e.id = b.event_id"
			+ " WHERE e.id IN ( "
			+ "SELECT `rowid`"
			+ " FROM events_titles"
			+ " WHERE events_titles MATCH :query || '*'"
			+ " UNION "
			+ "SELECT e.id"
			+ " FROM events e"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " WHERE t.name LIKE '%' || :query || '%'"
			+ " UNION "
			+ "SELECT ep.event_id"
			+ " FROM events_persons ep"
			+ " JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE p.name MATCH :query || '*'"
			+ " )"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC")
	public abstract DataSource.Factory<Integer, StatusEvent> getSearchResults(String query);

	/**
	 * Method called by SearchSuggestionProvider to return search results in the format expected by the search framework.
	 */
	@Query("SELECT e.id AS " + BaseColumns._ID
			+ ", et.title AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
			+ ", IFNULL(GROUP_CONCAT(p.name, ', '), '') || ' - ' || t.name AS " + SearchManager.SUGGEST_COLUMN_TEXT_2
			+ ", e.id AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA
			+ " FROM events e"
			+ " JOIN events_titles et ON e.id = et.`rowid`"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " LEFT JOIN events_persons ep ON e.id = ep.event_id"
			+ " LEFT JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE e.id IN ( "
			+ "SELECT `rowid`"
			+ " FROM events_titles"
			+ " WHERE events_titles MATCH :query || '*'"
			+ " UNION "
			+ "SELECT e.id"
			+ " FROM events e"
			+ " JOIN tracks t ON e.track_id = t.id"
			+ " WHERE t.name LIKE '%' || :query || '%'"
			+ " UNION "
			+ "SELECT ep.event_id"
			+ " FROM events_persons ep"
			+ " JOIN persons p ON ep.person_id = p.`rowid`"
			+ " WHERE p.name MATCH :query || '*'"
			+ " )"
			+ " GROUP BY e.id"
			+ " ORDER BY e.start_time ASC LIMIT :limit")
	@WorkerThread
	public abstract Cursor getSearchSuggestionResults(String query, int limit);

	/**
	 * Returns all persons in alphabetical order.
	 */
	@Query("SELECT `rowid`, name"
			+ " FROM persons"
			+ " ORDER BY name COLLATE NOCASE")
	public abstract DataSource.Factory<Integer, Person> getPersons();

	public LiveData<EventDetails> getEventDetails(final Event event) {
		final MutableLiveData<EventDetails> result = new MutableLiveData<>();
		appDatabase.getQueryExecutor().execute(() -> result.postValue(new EventDetails(getPersons(event), getLinks(event))));
		return result;
	}

	@Query("SELECT p.`rowid`, p.name"
			+ " FROM persons p"
			+ " JOIN events_persons ep ON p.`rowid` = ep.person_id"
			+ " WHERE ep.event_id = :event")
	protected abstract List<Person> getPersons(Event event);

	@Query("SELECT * FROM links WHERE event_id = :event ORDER BY id ASC")
	protected abstract List<Link> getLinks(Event event);
}
