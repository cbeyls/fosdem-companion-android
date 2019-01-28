package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;

import java.util.List;

public class TrackScheduleViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Pair<Day, Track>> dayTrack = new MutableLiveData<>();
	private final LiveData<List<StatusEvent>> schedule = Transformations.switchMap(dayTrack,
			new Function<Pair<Day, Track>, LiveData<List<StatusEvent>>>() {
				@Override
				public LiveData<List<StatusEvent>> apply(Pair<Day, Track> dayTrack) {
					return appDatabase.getScheduleDao().getEvents(dayTrack.first, dayTrack.second);
				}
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
}
