package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Person;

public class PersonInfoListFragment extends SmoothListFragment implements LoaderCallbacks<Cursor> {

	private static final int PERSON_EVENTS_LOADER_ID = 1;
	private static final String ARG_PERSON = "person";

	private Person person;
	private EventsAdapter adapter;

	public static PersonInfoListFragment newInstance(Person person) {
		PersonInfoListFragment f = new PersonInfoListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_PERSON, person);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new EventsAdapter(getActivity());
		person = getArguments().getParcelable(ARG_PERSON);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.person, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.more_info:
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(person.getUrl()));
				startActivity(intent);
				return true;
		}
		return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));

		int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
		ListView listView = getListView();
		listView.setPadding(contentMargin, contentMargin, contentMargin, contentMargin);
		listView.setClipToPadding(false);
		listView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

		View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_person_info, null);
		getListView().addHeaderView(headerView, null, false);

		setListAdapter(adapter);
		setListShown(false);

		getLoaderManager().initLoader(PERSON_EVENTS_LOADER_ID, null, this);
	}

	private static class PersonEventsLoader extends SimpleCursorLoader {

		private final Person person;

		public PersonEventsLoader(Context context, Person person) {
			super(context);
			this.person = person;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getEvents(person);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PersonEventsLoader(getActivity(), person);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		setListShown(true);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Event event = adapter.getItem(position - 1);
		Intent intent = new Intent(getActivity(), EventDetailsActivity.class).putExtra(EventDetailsActivity.EXTRA_EVENT, event);
		startActivity(intent);
	}
}
