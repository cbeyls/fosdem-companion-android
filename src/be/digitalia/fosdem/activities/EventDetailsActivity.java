package be.digitalia.fosdem.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.loaders.LocalCacheLoader;
import be.digitalia.fosdem.model.Event;

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 * 
 * @author Christophe Beyls
 * 
 */
public class EventDetailsActivity extends ActionBarActivity implements LoaderCallbacks<Event> {

	public static final String EXTRA_EVENT = "event";

	private static final int EVENT_LOADER_ID = 1;

	private Event event;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		getSupportActionBar().setTitle(R.string.event_details);

		event = getIntent().getParcelableExtra(EXTRA_EVENT);

		if (event != null) {
			// The event has been passed as parameter, it can be displayed immediately
			initActionBar();
			if (savedInstanceState == null) {
				Fragment f = EventDetailsFragment.newInstance(event);
				getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
			}
		} else {
			// Load the event from the DB using its id
			getSupportLoaderManager().initLoader(EVENT_LOADER_ID, null, this);
		}
	}

	/**
	 * Initialize event-related ActionBar configuration after the event has been loaded.
	 */
	private void initActionBar() {
		// Enable up navigation only after getting the event details
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Navigate up to the track associated to this event
			Intent upIntent = new Intent(this, TrackScheduleActivity.class);
			upIntent.putExtra(TrackScheduleActivity.EXTRA_DAY, event.getDay());
			upIntent.putExtra(TrackScheduleActivity.EXTRA_TRACK, event.getTrack());
			upIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			finish();
			if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
				TaskStackBuilder.create(this).addNextIntent(new Intent(this, MainActivity.class)).addNextIntent(upIntent).startActivities();
			} else {
				startActivity(upIntent);
			}
			return true;
		}
		return false;
	}

	private static class EventLoader extends LocalCacheLoader<Event> {

		private final long eventId;

		public EventLoader(Context context, long eventId) {
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
		return new EventLoader(this, Long.parseLong(getIntent().getDataString()));
	}

	@Override
	public void onLoadFinished(Loader<Event> loader, Event data) {
		if (data == null) {
			// Event not found, quit
			finish();
			return;
		}

		event = data;
		initActionBar();

		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentById(R.id.content) == null) {
			Fragment f = EventDetailsFragment.newInstance(event);
			fm.beginTransaction().add(R.id.content, f).commitAllowingStateLoss();
		}
	}

	@Override
	public void onLoaderReset(Loader<Event> loader) {
	}
}
