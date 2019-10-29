package be.digitalia.fosdem.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.SearchResultListFragment;
import be.digitalia.fosdem.viewmodels.SearchViewModel;
import com.google.android.material.snackbar.Snackbar;

public class SearchResultActivity extends AppCompatActivity {

	private static final String STATE_CURRENT_QUERY = "current_query";
	// Search Intent sent by Google Now
	private static final String GMS_ACTION_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION";

	private SearchViewModel viewModel;
	private SearchView searchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		viewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

		if (savedInstanceState == null) {
			SearchResultListFragment f = SearchResultListFragment.newInstance();
			getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commit();

			handleIntent(getIntent(), false);
		} else {
			viewModel.setQuery(savedInstanceState.getString(STATE_CURRENT_QUERY, ""));
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_CURRENT_QUERY, viewModel.getQuery());
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
