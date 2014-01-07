package be.digitalia.fosdem.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.SettingsFragment;

public class SettingsActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			SettingsFragment f = SettingsFragment.newInstance();
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
