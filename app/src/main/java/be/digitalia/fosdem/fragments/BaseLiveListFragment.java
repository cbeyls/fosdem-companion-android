package be.digitalia.fosdem.fragments;

import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.adapters.EventsAdapter;

public abstract class BaseLiveListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

	private static final int EVENTS_LOADER_ID = 1;

	private EventsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new EventsAdapter(getActivity(), this, false);
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

		LoaderManager.getInstance(this).initLoader(EVENTS_LOADER_ID, null, this);
	}

	protected abstract String getEmptyText();

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
