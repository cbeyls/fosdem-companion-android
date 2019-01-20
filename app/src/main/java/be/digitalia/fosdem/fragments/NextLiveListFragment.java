package be.digitalia.fosdem.fragments;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.viewmodels.LiveViewModel;

public class NextLiveListFragment extends BaseLiveListFragment {

	@Override
	protected String getEmptyText() {
		return getString(R.string.next_empty);
	}

	@NonNull
	@Override
	protected LiveData<PagedList<StatusEvent>> getDataSource(@NonNull LiveViewModel viewModel) {
		return viewModel.getNextEvents();
	}
}
