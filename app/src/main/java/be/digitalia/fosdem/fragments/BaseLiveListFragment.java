package be.digitalia.fosdem.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.viewmodels.LiveViewModel;

public abstract class BaseLiveListFragment extends RecyclerViewFragment implements Observer<PagedList<StatusEvent>> {

	private EventsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new EventsAdapter(getContext(), this, false);
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		Fragment parentFragment = getParentFragment();
		if (parentFragment instanceof RecycledViewPoolProvider) {
			recyclerView.setRecycledViewPool(((RecycledViewPoolProvider) parentFragment).getRecycledViewPool());
		}

		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getEmptyText());
		setProgressBarVisible(true);

		final LiveViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(LiveViewModel.class);
		getDataSource(viewModel).observe(getViewLifecycleOwner(), this);
	}

	private final Runnable preserveScrollPositionRunnable = () -> {
		// Ensure we stay at scroll position 0 so we can see the insertion animation
		final RecyclerView recyclerView = getRecyclerView();
		if (recyclerView != null) {
			if (recyclerView.getScrollY() == 0) {
				recyclerView.scrollToPosition(0);
			}
		}
	};

	@Override
	public void onChanged(PagedList<StatusEvent> events) {
		adapter.submitList(events, preserveScrollPositionRunnable);
		setProgressBarVisible(false);
	}

	protected abstract String getEmptyText();

	@NonNull
	protected abstract LiveData<PagedList<StatusEvent>> getDataSource(@NonNull LiveViewModel viewModel);
}
