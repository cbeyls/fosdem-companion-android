package be.digitalia.fosdem.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.PersonInfoListFragment;
import be.digitalia.fosdem.model.Person;

public class PersonInfoActivity extends ActionBarActivity {

	public static final String EXTRA_PERSON = "person";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		Person person = getIntent().getParcelableExtra(EXTRA_PERSON);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.person_info);

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
