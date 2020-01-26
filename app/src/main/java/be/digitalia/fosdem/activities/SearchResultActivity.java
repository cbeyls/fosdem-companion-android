package be.digitalia.fosdem.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.SearchResultListFragment;
import be.digitalia.fosdem.viewmodels.SearchViewModel;

public class SearchResultActivity extends SimpleToolbarActivity {

	// Search Intent sent by Google Now
	private static final String GMS_ACTION_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION";

	private SearchViewModel viewModel;
	private SearchView searchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

		if (savedInstanceState == null) {
			SearchResultListFragment f = SearchResultListFragment.newInstance();
			getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commit();

			handleIntent(getIntent(), false);
		}
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
			if (query == null) {
				query = "";
			} else {
				query = query.trim();
			}
			if (searchView != null) {
				setSearchViewQuery(query);
			}

			viewModel.setQuery(query);

			if (SearchViewModel.isQueryTooShort(query)) {
				TypedArray a = getTheme().obtainStyledAttributes(R.styleable.ErrorColors);
				int textColor = a.getColor(R.styleable.ErrorColors_colorOnError, 0);
				int backgroundColor = a.getColor(R.styleable.ErrorColors_colorError, 0);
				a.recycle();

				Snackbar.make(findViewById(R.id.content), R.string.search_length_error, Snackbar.LENGTH_LONG)
						.setTextColor(textColor)
						.setBackgroundTint(backgroundColor)
						.show();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search, menu);

		MenuItem searchMenuItem = menu.findItem(R.id.search);
		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) searchMenuItem.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(false); // Always show the search view
		setSearchViewQuery(viewModel.getQuery());

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
