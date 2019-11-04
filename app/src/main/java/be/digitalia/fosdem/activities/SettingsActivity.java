package be.digitalia.fosdem.activities;

import android.os.Bundle;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.SettingsFragment;

public class SettingsActivity extends SimpleToolbarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.content, new SettingsFragment())
					.commit();
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.partial_zoom_in, R.anim.slide_out_right);
	}
}
