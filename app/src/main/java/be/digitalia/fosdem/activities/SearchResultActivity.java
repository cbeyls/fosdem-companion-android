package be.digitalia.fosdem.activities;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.SearchResultListFragment;
import com.google.android.material.snackbar.Snackbar;

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
			if (searchView != null) {
				setSearchViewQuery(query);
			}
			final boolean isQueryTooShort = (query == null) || (query.length() < MIN_SEARCH_LENGTH);

			if (!ObjectsCompat.equals(currentQuery, query)) {
				currentQuery = query;
				FragmentManager fm = getSupportFragmentManager();
				if (isQueryTooShort) {
					Fragment f = fm.findFragmentById(R.id.content);
					if (f != null) {
						fm.beginTransaction().remove(f).commitAllowingStateLoss();
					}
				} else {
					SearchResultListFragment f = SearchResultListFragment.newInstance(query);
					fm.beginTransaction().replace(R.id.content, f).commitAllowingStateLoss();
				}
			}

			if (isQueryTooShort) {
				SpannableString errorMessage = new SpannableString(getString(R.string.search_length_error));
				int textColor = ContextCompat.getColor(this, R.color.error_material);
				errorMessage.setSpan(new ForegroundColorSpan(textColor), 0, errorMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				Snackbar.make(findViewById(R.id.content), errorMessage, Snackbar.LENGTH_LONG).show();
			}

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
