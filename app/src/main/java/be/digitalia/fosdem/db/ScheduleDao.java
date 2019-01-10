package be.digitalia.fosdem.db;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.db.entities.EventEntity;
import be.digitalia.fosdem.db.entities.EventTitles;
import be.digitalia.fosdem.db.entities.EventToPerson;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

@Dao
public abstract class ScheduleDao {

	public static final String ACTION_SCHEDULE_REFRESHED = BuildConfig.APPLICATION_ID + ".action.SCHEDULE_REFRESHED";

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
	public int storeSchedule(Context context, Iterable<DetailedEvent> events, String lastModifiedTag) {
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

			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_SCHEDULE_REFRESHED));
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

		for (DetailedEvent event : events) {
			// Retrieve or insert Track
			final Track track = event.getTrack();
			Long trackId = tracks.get(track);
			if (trackId == null) {
				// New track
				nextTrackId++;
				trackId = nextTrackId;
				track.setId(nextTrackId);
				insertTrack(track);
				tracks.put(track, trackId);
			} else {
				track.setId(trackId);
			}

			final long eventId = event.getId();
			try {
				// Insert main event and fulltext fields
				insertEvent(new EventEntity(event), new EventTitles(event));
			} catch (Exception e) {
				// Duplicate event: skip
				continue;
			}

			days.add(event.getDay());
			if (eventId < minEventId) {
				minEventId = eventId;
			}

			final List<Person> persons = event.getPersons();
			insertPersons(persons);
			final int personsCount = persons.size();
			final EventToPerson[] eventsToPersons = new EventToPerson[personsCount];
			for (int i = 0; i < personsCount; ++i) {
				eventsToPersons[i] = new EventToPerson(eventId, persons.get(i).getId());
			}
			insertEventsToPersons(eventsToPersons);

			insertLinks(event.getLinks());

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
	protected abstract long insertTrack(Track track);

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

		return DateUtils.getYear(date);
	}

	@Query("SELECT date FROM days ORDER BY `index` ASC LIMIT 1")
	protected abstract long getConferenceStartDate();

	@Query("SELECT * FROM tracks t"
			+ " JOIN events e ON t.id = e.track_id"
			+ " WHERE e.day_index = :day"
			+ " GROUP BY t.id"
			+ " ORDER BY t.name ASC")
	public abstract LiveData<List<Track>> getTracks(Day day);

	/**
	 * Returns all persons in alphabetical order.
	 */
	@Query("SELECT `rowid`, name"
			+ " FROM persons"
			+ " ORDER BY name COLLATE NOCASE")
	public abstract DataSource.Factory<Integer, Person> getPersons();

	/**
	 * Returns persons presenting the specified event.
	 */
	@Query("SELECT p.`rowid`, p.name"
			+ " FROM persons p"
			+ " JOIN events_persons ep ON p.`rowid` = ep.person_id"
			+ " WHERE ep.event_id = :event")
	public abstract LiveData<List<Person>> getPersons(Event event);

	@Query("SELECT * FROM links WHERE event_id = :event ORDER BY id ASC")
	public abstract LiveData<List<Link>> getLinks(Event event);
}
