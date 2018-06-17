package be.digitalia.fosdem.activities;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.MessageDialogFragment;
import be.digitalia.fosdem.fragments.SearchResultListFragment;

public class SearchResultActivity extends AppCompatActivity {

	public static final int MIN_SEARCH_LENGTH = 3;

	private static final String STATE_CURRENT_QUERY = "current_query";
	// Search Intent sent by Google Now
	private static final String GMS_ACTION_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION";

	private String currentQuery;
	private SearchView searchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			handleIntent(getIntent(), false);
		} else {
			currentQuery = savedInstanceState.getString(STATE_CURRENT_QUERY);
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
		if (Intent.ACTION_SEARCH.equals(intentAction) || GMS_ACTION_SEARCH.equals(intentAction)) {
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
			if (searchView != null) {
				setSearchViewQuery(query);
			}

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

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search, menu);

		MenuItem searchMenuItem = menu.findItem(R.id.search);
		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) searchMenuItem.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(false); // Always show the search view
		setSearchViewQuery(currentQuery);

		return true;
	}

	private void setSearchViewQuery(String query) {
		// Force losing the focus to prevent the suggestions from appearing
		searchView.clearFocus();
		searchView.setFocusable(false);
		searchView.setFocusableInTouchMode(false);
		searchView.setQuery(query, false);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}
