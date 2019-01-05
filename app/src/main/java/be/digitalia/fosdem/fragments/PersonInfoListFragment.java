package be.digitalia.fosdem.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.ConcatAdapter;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.utils.DateUtils;

public class PersonInfoListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

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

		adapter = new EventsAdapter(getActivity(), this);
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
				if (adapter.getItemCount() > 0) {
					final int year = DateUtils.getYear(adapter.getItem(0).getDay().getDate().getTime());
					String url = person.getUrl(year);
					if (url != null) {
						try {
							Activity context = getActivity();
							new CustomTabsIntent.Builder()
									.setToolbarColor(ContextCompat.getColor(context, R.color.color_primary))
									.setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
									.setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
									.build()
									.launchUrl(context, Uri.parse(url));
						} catch (ActivityNotFoundException ignore) {
						}
					}
				}
				return true;
		}
		return false;
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		final int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
		recyclerView.setPadding(contentMargin, contentMargin, contentMargin, contentMargin);
		recyclerView.setClipToPadding(false);
		recyclerView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(new ConcatAdapter(new HeaderAdapter(), adapter));
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		LoaderManager.getInstance(this).initLoader(PERSON_EVENTS_LOADER_ID, null, this);
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

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PersonEventsLoader(getActivity(), person);
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

	static class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {

		@Override
		public int getItemCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return R.layout.header_person_info;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_person_info, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			// Nothing to bind
		}

		static class ViewHolder extends RecyclerView.ViewHolder {

			ViewHolder(View itemView) {
				super(itemView);
			}
		}
	}
}
