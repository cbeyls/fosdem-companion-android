package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.StatusEvent;

public class SearchViewModel extends AndroidViewModel {

	private static final int MIN_SEARCH_LENGTH = 3;

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<String> query = new MutableLiveData<>();
	private final LiveData<PagedList<StatusEvent>> results = Transformations.switchMap(query,
			query -> {
				if (isQueryTooShort(query)) {
					MutableLiveData<PagedList<StatusEvent>> emptyResult = new MutableLiveData<>();
					emptyResult.setValue(null);
					return emptyResult;
				}
				return new LivePagedListBuilder<>(appDatabase.getScheduleDao().getSearchResults(query), 20)
						.build();
			});

	public SearchViewModel(@NonNull Application application) {
		super(application);
	}

	public void setQuery(@NonNull String query) {
		if (!query.equals(this.query.getValue())) {
			this.query.setValue(query);
		}
	}

	@Nullable
	public String getQuery() {
		return query.getValue();
	}

	public static boolean isQueryTooShort(String value) {
		return (value == null) || (value.length() < MIN_SEARCH_LENGTH);
	}

	public LiveData<PagedList<StatusEvent>> getResults() {
		return results;
	}
}
