package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;

public class SearchResultListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

	private static final int EVENTS_LOADER_ID = 1;
	private static final String ARG_QUERY = "query";

	private EventsAdapter adapter;

	public static SearchResultListFragment newInstance(String query) {
		SearchResultListFragment f = new SearchResultListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_QUERY, query);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new EventsAdapter(getActivity(), this);
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
		setEmptyText(getString(R.string.no_search_result));
		setProgressBarVisible(true);

		LoaderManager.getInstance(this).initLoader(EVENTS_LOADER_ID, null, this);
	}

	private static class TextSearchLoader extends SimpleCursorLoader {

		private final String query;

		public TextSearchLoader(Context context, String query) {
			super(context);
			this.query = query;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getSearchResults(query);
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String query = getArguments().getString(ARG_QUERY);
		return new TextSearchLoader(getActivity(), query);
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
