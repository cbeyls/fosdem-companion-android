package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.adapters.RecyclerViewCursorAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Person;

public class PersonsListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

	private static final int PERSONS_LOADER_ID = 1;

	private PersonsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PersonsAdapter(getActivity());
	}

	@NonNull
	@Override
	protected RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		return (RecyclerView) inflater.inflate(R.layout.recyclerview_fastscroll, container, false);
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
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		LoaderManager.getInstance(this).initLoader(PERSONS_LOADER_ID, null, this);
	}

	private static class PersonsLoader extends SimpleCursorLoader {

		PersonsLoader(Context context) {
			super(context);
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getPersons();
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PersonsLoader(getActivity());
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

	private static class PersonsAdapter extends RecyclerViewCursorAdapter<PersonViewHolder> {

		private final LayoutInflater inflater;

		PersonsAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		@Override
		public Person getItem(int position) {
			return DatabaseManager.toPerson((Cursor) super.getItem(position));
		}

		@NonNull
		@Override
		public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.simple_list_item_1_material, parent, false);
			return new PersonViewHolder(view);
		}

		@Override
		public void onBindViewHolder(PersonViewHolder holder, Cursor cursor) {
			holder.person = DatabaseManager.toPerson(cursor, holder.person);
			holder.textView.setText(holder.person.getName());
		}
	}

	static class PersonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		TextView textView;

		Person person;

		PersonViewHolder(@NonNull View itemView) {
			super(itemView);
			textView = itemView.findViewById(android.R.id.text1);
			itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View view) {
			final Context context = view.getContext();
			Intent intent = new Intent(context, PersonInfoActivity.class)
					.putExtra(PersonInfoActivity.EXTRA_PERSON, person);
			context.startActivity(intent);
		}
	}
}
