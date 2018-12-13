package be.digitalia.fosdem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.livedata.AsyncTaskLiveData;
import be.digitalia.fosdem.model.Event;

public class EventViewModel extends ViewModel {

	private long eventId = -1L;

	private final AsyncTaskLiveData<Event> event = new AsyncTaskLiveData<Event>() {
		@Override
		protected Event loadInBackground() throws Exception {
			return DatabaseManager.getInstance().getEvent(eventId);
		}
	};

	public boolean hasEventId() {
		return this.eventId != -1L;
	}

	public void setEventId(long eventId) {
		if (this.eventId != eventId) {
			this.eventId = eventId;
			event.forceLoad();
		}
	}

	public LiveData<Event> getEvent() {
		return event;
	}
}
