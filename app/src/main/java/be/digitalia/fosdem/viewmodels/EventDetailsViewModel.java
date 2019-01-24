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
import be.digitalia.fosdem.livedata.ExtraTransformations;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.EventDetails;

public class EventDetailsViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Event> event = new MutableLiveData<>();
	private final LiveData<Boolean> bookmarkStatus = Transformations.switchMap(event,
			new Function<Event, LiveData<Boolean>>() {
				@Override
				public LiveData<Boolean> apply(Event event) {
					// Prevent animating the UI when a bookmark is added back or removed back
					return ExtraTransformations.distinctUntilChanged(
							appDatabase.getBookmarksDao().getBookmarkStatus(event)
					);
				}
			});
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

	public LiveData<Boolean> getBookmarkStatus() {
		return bookmarkStatus;
	}

	public void toggleBookmarkStatus() {
		final Event event = this.event.getValue();
		final Boolean isBookmarked = bookmarkStatus.getValue();
		if (event != null && isBookmarked != null) {
			AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
				@Override
				public void run() {
					if (isBookmarked) {
						appDatabase.getBookmarksDao().removeBookmark(event);
					} else {
						appDatabase.getBookmarksDao().addBookmark(event);
					}
				}
			});
		}
	}

	public LiveData<EventDetails> getEventDetails() {
		return eventDetails;
	}
}
