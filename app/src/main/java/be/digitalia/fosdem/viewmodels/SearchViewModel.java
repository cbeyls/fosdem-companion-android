package be.digitalia.fosdem.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.StatusEvent;

public class SearchViewModel extends AndroidViewModel {

	private static final int MIN_SEARCH_LENGTH = 3;
	private static final String STATE_QUERY = "query";

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final SavedStateHandle state;
	private final LiveData<String> query;
	private final LiveData<PagedList<StatusEvent>> results;

	public SearchViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
		super(application);
		this.state = state;
		query = state.getLiveData(STATE_QUERY);
		results = Transformations.switchMap(query,
				query -> {
					if (isQueryTooShort(query)) {
						MutableLiveData<PagedList<StatusEvent>> emptyResult = new MutableLiveData<>();
						emptyResult.setValue(null);
						return emptyResult;
					}
					return new LivePagedListBuilder<>(appDatabase.getScheduleDao().getSearchResults(query), 20)
							.build();
				});
	}

	public void setQuery(@NonNull String query) {
		if (!query.equals(this.query.getValue())) {
			state.set(STATE_QUERY, query);
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
