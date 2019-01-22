package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.BookmarksAdapter;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.providers.BookmarksExportProvider;
import be.digitalia.fosdem.viewmodels.BookmarksViewModel;
import be.digitalia.fosdem.widgets.MultiChoiceHelper;

import java.util.List;

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
public class BookmarksListFragment extends RecyclerViewFragment implements Observer<List<Event>> {

	private static final String PREF_UPCOMING_ONLY = "bookmarks_upcoming_only";
	private static final String STATE_ADAPTER = "adapter";

	private BookmarksViewModel viewModel;
	private BookmarksAdapter adapter;

	private MenuItem filterMenuItem;
	private MenuItem upcomingOnlyMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		viewModel = ViewModelProviders.of(this).get(BookmarksViewModel.class);
		final MultiChoiceHelper.MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceHelper.MultiChoiceModeListener() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.action_mode_bookmarks, menu);
				return true;
			}

			private void updateSelectedCountDisplay(ActionMode mode) {
				int count = adapter.getMultiChoiceHelper().getCheckedItemCount();
				mode.setTitle(getResources().getQuantityString(R.plurals.selected, count, count));
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				updateSelectedCountDisplay(mode);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
					case R.id.delete:
						// Remove multiple bookmarks at once
						viewModel.removeBookmarks(adapter.getMultiChoiceHelper().getCheckedItemIds());
						mode.finish();
						return true;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				updateSelectedCountDisplay(mode);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}
		};
		adapter = new BookmarksAdapter((AppCompatActivity) getActivity(), this, multiChoiceModeListener);
		if (savedInstanceState != null) {
			adapter.getMultiChoiceHelper().onRestoreInstanceState(savedInstanceState.getParcelable(STATE_ADAPTER));
		}
		boolean upcomingOnly = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false);
		viewModel.setUpcomingOnly(upcomingOnly);

		setHasOptionsMenu(true);
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

		viewModel.getBookmarks().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_ADAPTER, adapter.getMultiChoiceHelper().onSaveInstanceState());
	}

	@Override
	public void onDestroyView() {
		adapter.getMultiChoiceHelper().clearChoices();
		super.onDestroyView();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.bookmarks, menu);
		filterMenuItem = menu.findItem(R.id.filter);
		upcomingOnlyMenuItem = menu.findItem(R.id.upcoming_only);
		updateFilterMenuItem();
	}

	private void updateFilterMenuItem() {
		if (filterMenuItem != null) {
			final boolean upcomingOnly = viewModel.getUpcomingOnly();
			filterMenuItem.setIcon(upcomingOnly ?
					R.drawable.ic_filter_list_selected_white_24dp
					: R.drawable.ic_filter_list_white_24dp);
			upcomingOnlyMenuItem.setChecked(upcomingOnly);
		}
	}

	@Override
	public void onDestroyOptionsMenu() {
		super.onDestroyOptionsMenu();
		filterMenuItem = null;
		upcomingOnlyMenuItem = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.upcoming_only:
				final boolean upcomingOnly = !viewModel.getUpcomingOnly();
				viewModel.setUpcomingOnly(upcomingOnly);
				updateFilterMenuItem();
				getActivity().getPreferences(Context.MODE_PRIVATE).edit()
						.putBoolean(PREF_UPCOMING_ONLY, upcomingOnly)
						.apply();
				return true;
			case R.id.export_bookmarks:
				Intent exportIntent = BookmarksExportProvider.getIntent(getActivity());
				startActivity(Intent.createChooser(exportIntent, getString(R.string.export_bookmarks)));
				return true;
		}
		return false;
	}

	@Override
	public void onChanged(List<Event> bookmarks) {
		adapter.submitList(bookmarks);
		setProgressBarVisible(false);
	}
}
