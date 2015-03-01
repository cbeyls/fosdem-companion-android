package be.digitalia.fosdem.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.PersonInfoListFragment;
import be.digitalia.fosdem.model.KeySpeaker;

public class PersonInfoActivity extends ActionBarActivity {

    public static final String SPEAKER = "speaker";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content_extended_title);
        KeySpeaker person = getIntent().getParcelableExtra(SPEAKER);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(person.getName());

		if (savedInstanceState == null) {
			Fragment f = PersonInfoListFragment.newInstance(person);
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
