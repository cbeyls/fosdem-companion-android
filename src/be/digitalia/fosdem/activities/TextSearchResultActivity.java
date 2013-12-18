package be.digitalia.fosdem.activities;

import be.digitalia.fosdem.R;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class TextSearchResultActivity extends ActionBarActivity {

	public static final int MIN_SEARCH_LENGTH = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.search_events);

		if (savedInstanceState == null) {
			handleIntent(getIntent());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			return;
		}

		String query = intent.getStringExtra(SearchManager.QUERY);
		if (query != null) {
			query = query.trim();
		}
		if ((query == null) || (query.length() < MIN_SEARCH_LENGTH)) {
			// MessageDialogFragment.newInstance(R.string.error_title, R.string.search_length_error).show(getSupportFragmentManager());
			return;
		}

		getSupportActionBar().setSubtitle(query);

		// TextSearchResultFragment f = TextSearchResultFragment.newInstance(query);
		// getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.search:
			onSearchRequested();
			return true;
		}
		return false;
	}
}
