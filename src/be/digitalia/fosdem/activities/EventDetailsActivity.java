package be.digitalia.fosdem.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.loaders.LocalCacheLoader;
import be.digitalia.fosdem.model.Event;

public class EventDetailsActivity extends ActionBarActivity implements LoaderCallbacks<Event> {

	public static final String EXTRA_EVENT = "event";
	public static final String EXTRA_EVENT_ID = "event_id";

	private static final int EVENT_LOADER_ID = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.event_details);

		Event event = getIntent().getParcelableExtra(EXTRA_EVENT);

		if (event != null) {
			// The event has been passed as parameter, it can be displayed immediately
			if (savedInstanceState == null) {
				Fragment f = EventDetailsFragment.newInstance(event);
				getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
			}
		} else {
			// We need to load the event from the DB using its id
			if ((savedInstanceState == null) || (getSupportFragmentManager().findFragmentById(R.id.content) == null)) {
				getSupportLoaderManager().initLoader(EVENT_LOADER_ID, null, this);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

	private static class EventLoader extends LocalCacheLoader<Event> {

		private final int eventId;

		public EventLoader(Context context, int eventId) {
			super(context);
			this.eventId = eventId;
		}

		@Override
		public Event loadInBackground() {
			return DatabaseManager.getInstance().getEvent(eventId);
		}
	}

	@Override
	public Loader<Event> onCreateLoader(int id, Bundle args) {
		return new EventLoader(this, getIntent().getIntExtra(EXTRA_EVENT_ID, -1));
	}

	@Override
	public void onLoadFinished(Loader<Event> loader, Event event) {
		if (event == null) {
			// Event not found
			finish();
			return;
		}
		Fragment f = EventDetailsFragment.newInstance(event);
		getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commitAllowingStateLoss();
	}

	@Override
	public void onLoaderReset(Loader<Event> loader) {
	}
}
