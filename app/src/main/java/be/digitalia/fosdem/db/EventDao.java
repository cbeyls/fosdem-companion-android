package be.digitalia.fosdem.db;

import java.util.List;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import be.digitalia.fosdem.db.entities.EventEntity;
import be.digitalia.fosdem.db.entities.EventTitles;
import be.digitalia.fosdem.db.entities.EventToPerson;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;
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

	@Query("DELETE FROM " + EventEntity.TABLE_NAME)
	protected abstract void clearEvents();

	@Query("DELETE FROM " + EventTitles.TABLE_NAME)
	protected abstract void clearEventTitles();

	@Query("DELETE FROM " + Person.TABLE_NAME)
	protected abstract void clearPersons();

	@Query("DELETE FROM " + EventToPerson.TABLE_NAME)
	protected abstract void clearEventToPersons();

	@Query("DELETE FROM " + Link.TABLE_NAME)
	protected abstract void clearLinks();

	@Query("DELETE FROM " + Track.TABLE_NAME)
	protected abstract void clearTracks();

	@Query("DELETE FROM " + Day.TABLE_NAME)
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

	@Query("SELECT _index, date FROM " + Day.TABLE_NAME + " ORDER BY _index ASC")
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

	@Query("SELECT date FROM " + Day.TABLE_NAME + " ORDER BY _index ASC LIMIT 1")
	protected abstract long getConferenceStartDate();
}
