package be.digitalia.fosdem.db;

import java.util.List;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.utils.DateUtils;

@Dao
public abstract class EventDao {

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
