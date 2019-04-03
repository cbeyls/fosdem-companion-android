package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.StatusEvent;

import java.util.Arrays;

public class ExternalBookmarksViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<long[]> bookmarkIds = new MutableLiveData<>();
	private final LiveData<PagedList<StatusEvent>> bookmarks = Transformations.switchMap(bookmarkIds,
			bookmarkIds -> new LivePagedListBuilder<>(appDatabase.getScheduleDao().getEvents(bookmarkIds), 20)
					.build());

	public ExternalBookmarksViewModel(@NonNull Application application) {
		super(application);
	}

	public void setBookmarkIds(@NonNull long[] bookmarkIds) {
		if (!Arrays.equals(bookmarkIds, this.bookmarkIds.getValue())) {
			this.bookmarkIds.setValue(bookmarkIds);
		}
	}

	public LiveData<PagedList<StatusEvent>> getBookmarks() {
		return bookmarks;
	}
}
