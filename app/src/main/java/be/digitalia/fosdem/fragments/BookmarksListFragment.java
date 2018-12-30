package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.BookmarksAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.providers.BookmarksExportProvider;

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
public class BookmarksListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

	private static final int BOOKMARKS_LOADER_ID = 1;
	private static final String PREF_UPCOMING_ONLY = "bookmarks_upcoming_only";
	private static final String STATE_ADAPTER = "adapter";

	private BookmarksAdapter adapter;
	private boolean upcomingOnly;

	private MenuItem filterMenuItem;
	private MenuItem upcomingOnlyMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new BookmarksAdapter((AppCompatActivity) getActivity(), this);
		if (savedInstanceState != null) {
			adapter.onRestoreInstanceState(savedInstanceState.getParcelable(STATE_ADAPTER));
		}
		upcomingOnly = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false);

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

		LoaderManager.getInstance(this).initLoader(BOOKMARKS_LOADER_ID, null, this);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_ADAPTER, adapter.onSaveInstanceState());
	}

	@Override
	public void onDestroyView() {
		adapter.onDestroyView();
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
				upcomingOnly = !upcomingOnly;
				updateFilterMenuItem();
				getActivity().getPreferences(Context.MODE_PRIVATE).edit()
						.putBoolean(PREF_UPCOMING_ONLY, upcomingOnly)
						.apply();
				LoaderManager.getInstance(this).restartLoader(BOOKMARKS_LOADER_ID, null, this);
				return true;
			case R.id.export_bookmarks:
				Intent exportIntent = BookmarksExportProvider.getIntent(getActivity());
				startActivity(Intent.createChooser(exportIntent, getString(R.string.export_bookmarks)));
				return true;
		}
		return false;
	}

	private static class BookmarksLoader extends SimpleCursorLoader {

		// Events that just started are still shown for 5 minutes
		private static final long TIME_OFFSET = 5L * DateUtils.MINUTE_IN_MILLIS;

		private final boolean upcomingOnly;
		private final Handler handler;
		private final Runnable timeoutRunnable = new Runnable() {

			@Override
			public void run() {
				onContentChanged();
			}
		};

		public BookmarksLoader(Context context, boolean upcomingOnly) {
			super(context);
			this.upcomingOnly = upcomingOnly;
			this.handler = new Handler();
		}

		@Override
		public void deliverResult(Cursor cursor) {
			if (upcomingOnly && !isReset()) {
				handler.removeCallbacks(timeoutRunnable);
				// The loader will be refreshed when the start time of the first bookmark in the list is reached
				if ((cursor != null) && cursor.moveToFirst()) {
					long startTime = DatabaseManager.toEventStartTimeMillis(cursor);
					if (startTime != -1L) {
						long delay = startTime - (System.currentTimeMillis() - TIME_OFFSET);
						if (delay > 0L) {
							handler.postDelayed(timeoutRunnable, delay);
						} else {
							onContentChanged();
						}
					}
				}
			}
			super.deliverResult(cursor);
		}

		@Override
		protected void onReset() {
			super.onReset();
			if (upcomingOnly) {
				handler.removeCallbacks(timeoutRunnable);
			}
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getBookmarks(upcomingOnly ? System.currentTimeMillis() - TIME_OFFSET : 0L);
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new BookmarksLoader(getActivity(), upcomingOnly);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		setProgressBarVisible(false);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
}
