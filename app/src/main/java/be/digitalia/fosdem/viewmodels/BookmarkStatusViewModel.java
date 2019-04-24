package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.BookmarkStatus;
import be.digitalia.fosdem.model.Event;

public class BookmarkStatusViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Event> event = new MutableLiveData<>();
	private final LiveData<BookmarkStatus> bookmarkStatus = Transformations.switchMap(event,
			new Function<Event, LiveData<BookmarkStatus>>() {
				@Override
				public LiveData<BookmarkStatus> apply(Event event) {
					if (event == null) {
						MutableLiveData<BookmarkStatus> singleNullResult = new MutableLiveData<>();
						singleNullResult.setValue(null);
						return singleNullResult;
					}

					return Transformations.map(
							// Prevent updating the UI when a bookmark is added back or removed back
							Transformations.distinctUntilChanged(
									appDatabase.getBookmarksDao().getBookmarkStatus(event)
							), isBookmarked -> {
								if (isBookmarked == null) {
									return null;
								}
								final boolean isUpdate = firstResultReceived;
								firstResultReceived = true;
								return new BookmarkStatus(isBookmarked, isUpdate);
							}
					);
				}
			});
	private boolean firstResultReceived = false;

	public BookmarkStatusViewModel(@NonNull Application application) {
		super(application);
	}

	public void setEvent(Event event) {
		if (!ObjectsCompat.equals(event, this.event.getValue())) {
			firstResultReceived = false;
			this.event.setValue(event);
		}
	}

	@Nullable
	public Event getEvent() {
		return event.getValue();
	}

	public LiveData<BookmarkStatus> getBookmarkStatus() {
		return bookmarkStatus;
	}

	public void toggleBookmarkStatus() {
		final Event event = this.event.getValue();
		final BookmarkStatus currentStatus = bookmarkStatus.getValue();
		// Ignore the action if the status for the current event hasn't been received yet
		if (event != null && currentStatus != null && firstResultReceived) {
			AsyncTask.SERIAL_EXECUTOR.execute(() -> {
				if (currentStatus.isBookmarked()) {
					appDatabase.getBookmarksDao().removeBookmark(event);
				} else {
					appDatabase.getBookmarksDao().addBookmark(event);
				}
			});
		}
	}
}
