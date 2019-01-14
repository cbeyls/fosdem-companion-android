package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Event;

public class EventViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Long> eventId = new MutableLiveData<>();
	private final LiveData<Event> event = Transformations.switchMap(eventId,
			new Function<Long, LiveData<Event>>() {
				@Override
				public LiveData<Event> apply(final Long id) {
					final MutableLiveData<Event> resultLiveData = new MutableLiveData<>();
					AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
						@Override
						public void run() {
							final Event result = appDatabase.getScheduleDao().getEvent(id);
							resultLiveData.postValue(result);
						}
					});
					return resultLiveData;
				}
			});

	public EventViewModel(@NonNull Application application) {
		super(application);
	}

	public boolean hasEventId() {
		return this.eventId.getValue() != null;
	}

	public void setEventId(long eventId) {
		Long newEventId = eventId;
		if (!newEventId.equals(this.eventId.getValue())) {
			this.eventId.setValue(newEventId);
		}
	}

	public LiveData<Event> getEvent() {
		return event;
	}
}
