package be.digitalia.fosdem.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.TrackScheduleListFragment;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Track;

public class TrackScheduleActivity extends ActionBarActivity {

	public static final String ARG_DAY = "day";
	public static final String ARG_TRACK = "track";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		Bundle extras = getIntent().getExtras();
		Day day = extras.getParcelable(ARG_DAY);
		Track track = extras.getParcelable(ARG_TRACK);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(track.toString());
		bar.setSubtitle(day.toString());

		if (savedInstanceState == null) {
			Fragment f = TrackScheduleListFragment.newInstance(day, track);
			getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
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
}
