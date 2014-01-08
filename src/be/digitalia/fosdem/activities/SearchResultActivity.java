package be.digitalia.fosdem.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.MessageDialogFragment;
import be.digitalia.fosdem.fragments.SearchResultListFragment;

public class SearchResultActivity extends ActionBarActivity {

	public static final int MIN_SEARCH_LENGTH = 3;

	private static final String STATE_CURRENT_QUERY = "current_query";

	private String currentQuery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.search_events);

		if (savedInstanceState == null) {
			handleIntent(getIntent(), false);
		} else {
			currentQuery = savedInstanceState.getString(STATE_CURRENT_QUERY);
			getSupportActionBar().setSubtitle(currentQuery);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_CURRENT_QUERY, currentQuery);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent, true);
	}

	private void handleIntent(Intent intent, boolean isNewIntent) {
		String intentAction = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(intentAction)) {
			// Normal search, results are displayed here
			String query = intent.getStringExtra(SearchManager.QUERY);
			if (query != null) {
				query = query.trim();
			}
			if ((query == null) || (query.length() < MIN_SEARCH_LENGTH)) {
				MessageDialogFragment.newInstance(R.string.error_title, R.string.search_length_error).show(getSupportFragmentManager());
				return;
			}

			currentQuery = query;
			getSupportActionBar().setSubtitle(query);

			SearchResultListFragment f = SearchResultListFragment.newInstance(query);
			getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commit();

		} else if (Intent.ACTION_VIEW.equals(intentAction)) {
			// Search suggestion, dispatch to EventDetailsActivity
			Intent dispatchIntent = new Intent(this, EventDetailsActivity.class).setData(intent.getData());
			startActivity(dispatchIntent);

			if (!isNewIntent) {
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search, menu);
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
