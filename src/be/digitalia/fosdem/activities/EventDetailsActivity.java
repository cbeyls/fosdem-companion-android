package be.digitalia.fosdem.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Event;

public class EventDetailsActivity extends ActionBarActivity {

	public static final String EXTRA_EVENT = "event";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		Event event = getIntent().getParcelableExtra(EXTRA_EVENT);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.event_details);

		if (savedInstanceState == null) {
			Fragment f = EventDetailsFragment.newInstance(event);
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
