package be.digitalia.fosdem.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.viewmodels.ExternalBookmarksViewModel;

public class ExternalBookmarksListFragment extends RecyclerViewFragment implements Observer<PagedList<StatusEvent>> {

	private static final String ARG_BOOKMARK_IDS = "bookmark_ids";

	private EventsAdapter adapter;

	public static ExternalBookmarksListFragment newInstance(@NonNull long[] bookmarkIds) {
		ExternalBookmarksListFragment f = new ExternalBookmarksListFragment();
		Bundle args = new Bundle();
		args.putLongArray(ARG_BOOKMARK_IDS, bookmarkIds);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new EventsAdapter(getContext(), this);
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getString(R.string.no_bookmark));
		setProgressBarVisible(true);

		long[] bookmarkIds = requireArguments().getLongArray(ARG_BOOKMARK_IDS);
		final ExternalBookmarksViewModel viewModel = new ViewModelProvider(this).get(ExternalBookmarksViewModel.class);
		viewModel.setBookmarkIds(bookmarkIds);
		viewModel.getBookmarks().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onChanged(PagedList<StatusEvent> bookmarks) {
		adapter.submitList(bookmarks);
		setProgressBarVisible(false);
	}
}
