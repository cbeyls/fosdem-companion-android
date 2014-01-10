package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.fragments.RoomImageDialogFragment;
import be.digitalia.fosdem.fragments.TrackScheduleListFragment;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;

/**
 * Track Schedule container, works in both single pane and dual pane modes.
 * 
 * @author Christophe Beyls
 * 
 */
public class TrackScheduleActivity extends ActionBarActivity implements TrackScheduleListFragment.Callbacks {

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_TRACK = "track";

	private Day day;
	private Track track;
	private boolean isTabletLandscape;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_schedule);

		Bundle extras = getIntent().getExtras();
		day = extras.getParcelable(EXTRA_DAY);
		track = extras.getParcelable(EXTRA_TRACK);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(track.toString());
		bar.setSubtitle(day.toString());

		isTabletLandscape = getResources().getBoolean(R.bool.tablet_landscape);

		TrackScheduleListFragment trackScheduleListFragment;
		FragmentManager fm = getSupportFragmentManager();
		if (savedInstanceState == null) {
			trackScheduleListFragment = TrackScheduleListFragment.newInstance(day, track);
			fm.beginTransaction().add(R.id.schedule, trackScheduleListFragment).commit();
		} else {
			trackScheduleListFragment = (TrackScheduleListFragment) fm.findFragmentById(R.id.schedule);

			// Remove the room image DialogFragment when switching from dual pane to single pane mode
			if (!isTabletLandscape) {
				Fragment roomImageDialogFragment = fm.findFragmentByTag(RoomImageDialogFragment.TAG);
				if (roomImageDialogFragment != null) {
					fm.beginTransaction().remove(roomImageDialogFragment).commit();
				}
			}
		}
		trackScheduleListFragment.setSelectionEnabled(isTabletLandscape);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return false;
	}

	@Override
	public void onEventSelected(int position, Event event) {
		if (isTabletLandscape) {
			// Tablet mode: Show event details in the right pane fragment
			FragmentManager fm = getSupportFragmentManager();
			EventDetailsFragment currentFragment = (EventDetailsFragment) fm.findFragmentById(R.id.event);
			if (event != null) {
				// Only replace the fragment if the event is different
				if ((currentFragment == null) || !currentFragment.getEvent().equals(event)) {
					Fragment f = EventDetailsFragment.newInstance(event);
					// Allow state loss since the event fragment will be synchronized with the list selection after activity re-creation
					fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.event, f).commitAllowingStateLoss();
				}
			} else {
				// Nothing is selected because the list is empty
				if (currentFragment != null) {
					fm.beginTransaction().remove(currentFragment).commitAllowingStateLoss();
				}
			}
		} else {
			// Classic mode: Show event details in a new activity
			Intent intent = new Intent(this, TrackScheduleEventActivity.class);
			intent.putExtra(TrackScheduleEventActivity.EXTRA_DAY, day);
			intent.putExtra(TrackScheduleEventActivity.EXTRA_TRACK, track);
			intent.putExtra(TrackScheduleEventActivity.EXTRA_POSITION, position);
			startActivity(intent);
		}
	}
}
