package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Person;

public class PersonsListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int PERSONS_LOADER_ID = 1;

	private PersonsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PersonsAdapter(getActivity());
		setListAdapter(adapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));
		setListShown(false);

		getLoaderManager().initLoader(PERSONS_LOADER_ID, null, this);
	}

	private static class HistoryLoader extends SimpleCursorLoader {

		public HistoryLoader(Context context) {
			super(context);
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getPersons();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new HistoryLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO
	}

	private static class PersonsAdapter extends CursorAdapter {

		private final LayoutInflater inflater;

		public PersonsAdapter(Context context) {
			super(context, null, 0);
			inflater = LayoutInflater.from(context);
		}

		@Override
		public Person getItem(int position) {
			return DatabaseManager.toPerson((Cursor) super.getItem(position));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.textView = (TextView) view.findViewById(android.R.id.text1);
			view.setTag(holder);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.person = DatabaseManager.toPerson(cursor, holder.person);
			holder.textView.setText(holder.person.getName());
		}

		private static class ViewHolder {
			public TextView textView;
			public Person person;
		}
	}
}
