package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.livedata.LiveDataFactory;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;

public class TrackScheduleViewModel extends AndroidViewModel {

	private static final long REFRESH_TIME_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Pair<Day, Track>> dayTrack = new MutableLiveData<>();

	private final LiveData<List<StatusEvent>> schedule = Transformations.switchMap(dayTrack,
			dayTrack -> appDatabase.getScheduleDao().getEvents(dayTrack.first, dayTrack.second));

	// Auto refresh during the day passed as argument
	private final LiveData<Boolean> scheduler = Transformations.switchMap(dayTrack, dayTrack -> {
		final long dayStart = dayTrack.first.getDate().getTime();
		return LiveDataFactory.scheduler(dayStart, dayStart + android.text.format.DateUtils.DAY_IN_MILLIS);
	});
	private final LiveData<Long> currentTime = Transformations.switchMap(scheduler, isOn -> {
		if (isOn) {
			return Transformations.map(
					LiveDataFactory.interval(REFRESH_TIME_INTERVAL, TimeUnit.MILLISECONDS),
					count -> System.currentTimeMillis()
			);
		}
		return new MutableLiveData<>(-1L);
	});

	public TrackScheduleViewModel(@NonNull Application application) {
		super(application);
	}

	public void setTrack(@NonNull Day day, @NonNull Track track) {
		Pair<Day, Track> dayTrack = Pair.create(day, track);
		if (!dayTrack.equals(this.dayTrack.getValue())) {
			this.dayTrack.setValue(dayTrack);
		}
	}

	public LiveData<List<StatusEvent>> getSchedule() {
		return schedule;
	}

	/**
	 * @return The current time during the target day, or -1 outside of the target day.
	 */
	public LiveData<Long> getCurrentTime() {
		return currentTime;
	}
}
