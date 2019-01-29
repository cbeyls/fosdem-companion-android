package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.EventDetails;

public class EventDetailsViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Event> event = new MutableLiveData<>();
	private final LiveData<EventDetails> eventDetails = Transformations.switchMap(event,
			new Function<Event, LiveData<EventDetails>>() {
				@Override
				public LiveData<EventDetails> apply(Event event) {
					return appDatabase.getScheduleDao().getEventDetails(event);
				}
			});

	public EventDetailsViewModel(@NonNull Application application) {
		super(application);
	}

	public void setEvent(@NonNull Event event) {
		if (!event.equals(this.event.getValue())) {
			this.event.setValue(event);
		}
	}

	public LiveData<EventDetails> getEventDetails() {
		return eventDetails;
	}
}
