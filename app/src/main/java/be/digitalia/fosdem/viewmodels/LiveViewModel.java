package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.livedata.LiveDataFactory;
import be.digitalia.fosdem.model.StatusEvent;

import java.util.concurrent.TimeUnit;

public class LiveViewModel extends AndroidViewModel {

	static final long NEXT_EVENTS_INTERVAL = 30L * DateUtils.MINUTE_IN_MILLIS;

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final LiveData<Long> heartbeat = LiveDataFactory.interval(1L, TimeUnit.MINUTES);
	private final LiveData<PagedList<StatusEvent>> nextEvents = Transformations.switchMap(heartbeat,
			version -> {
				final long now = System.currentTimeMillis();
				return new LivePagedListBuilder<>(appDatabase.getScheduleDao().getEventsWithStartTime(now, now + NEXT_EVENTS_INTERVAL), 20)
						.build();
			});
	private final LiveData<PagedList<StatusEvent>> eventsInProgress = Transformations.switchMap(heartbeat,
			version -> {
				final long now = System.currentTimeMillis();
				return new LivePagedListBuilder<>(appDatabase.getScheduleDao().getEventsInProgress(now), 20)
						.build();
			});

	public LiveViewModel(@NonNull Application application) {
		super(application);
	}

	public LiveData<PagedList<StatusEvent>> getNextEvents() {
		return nextEvents;
	}

	public LiveData<PagedList<StatusEvent>> getEventsInProgress() {
		return eventsInProgress;
	}
}
