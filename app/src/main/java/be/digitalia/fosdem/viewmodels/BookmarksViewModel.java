package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.livedata.LiveDataFactory;
import be.digitalia.fosdem.model.Event;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BookmarksViewModel extends AndroidViewModel {

	// In upcomingOnly mode, events that just started are still shown for 5 minutes
	static final long TIME_OFFSET = 5L * DateUtils.MINUTE_IN_MILLIS;

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Boolean> upcomingOnly = new MutableLiveData<>();
	private final LiveData<List<Event>> bookmarks = Transformations.switchMap(upcomingOnly,
			upcomingOnly -> {
				if (upcomingOnly == Boolean.TRUE) {
					// Refresh upcoming bookmarks every 2 minutes
					final LiveData<Long> heartbeat = LiveDataFactory.interval(2L, TimeUnit.MINUTES);
					return Transformations.switchMap(heartbeat,
							version -> appDatabase.getBookmarksDao().getBookmarks(System.currentTimeMillis() - TIME_OFFSET));
				}

				return appDatabase.getBookmarksDao().getBookmarks(-1L);
			});

	public BookmarksViewModel(@NonNull Application application) {
		super(application);
	}

	public void setUpcomingOnly(boolean upcomingOnly) {
		final Boolean boxedUpcomingOnly = upcomingOnly;
		if (!boxedUpcomingOnly.equals(this.upcomingOnly.getValue())) {
			this.upcomingOnly.setValue(boxedUpcomingOnly);
		}
	}

	public boolean getUpcomingOnly() {
		return Boolean.TRUE.equals(this.upcomingOnly.getValue());
	}

	public LiveData<List<Event>> getBookmarks() {
		return bookmarks;
	}

	public void removeBookmarks(final long[] eventIds) {
		AsyncTask.SERIAL_EXECUTOR.execute(() -> appDatabase.getBookmarksDao().removeBookmarks(eventIds));
	}
}
