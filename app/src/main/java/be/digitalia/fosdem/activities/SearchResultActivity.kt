package be.digitalia.fosdem.activities

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import androidx.lifecycle.observe
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.SearchResultListFragment
import be.digitalia.fosdem.viewmodels.SearchViewModel
import be.digitalia.fosdem.viewmodels.SearchViewModel.Result.QueryTooShort
import com.google.android.material.snackbar.Snackbar

class SearchResultActivity : SimpleToolbarActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commit { add(R.id.content, SearchResultListFragment.newInstance()) }
            handleIntent(intent, false)
        }

        viewModel.results.observe(this) { result ->
            if (result is QueryTooShort) {
                theme.obtainStyledAttributes(R.styleable.ErrorColors).apply {
                    val textColor = getColor(R.styleable.ErrorColors_colorOnError, 0)
                    val backgroundColor = getColor(R.styleable.ErrorColors_colorError, 0)
                    recycle()

                    Snackbar.make(findViewById(R.id.content), R.string.search_length_error, Snackbar.LENGTH_LONG)
                            .setTextColor(textColor)
                            .setBackgroundTint(backgroundColor)
                            .show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, true)
    }

    private fun handleIntent(intent: Intent, isNewIntent: Boolean) {
        when (intent.action) {
            Intent.ACTION_SEARCH, GMS_ACTION_SEARCH -> {
                // Normal search, results are displayed here
                val query = intent.getStringExtra(SearchManager.QUERY)?.trim() ?: ""
                searchView?.setQueryWithoutFocus(query)
                viewModel.query = query
            }
            Intent.ACTION_VIEW -> {
                // Search suggestion, dispatch to EventDetailsActivity
                val dispatchIntent = Intent(this, EventDetailsActivity::class.java).setData(intent.data)
                startActivity(dispatchIntent)
                if (!isNewIntent) {
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)

        menu.findItem(R.id.search)?.apply {
            // Associate searchable configuration with the SearchView
            val searchManager: SearchManager? = getSystemService()
            searchView = (actionView as SearchView).apply {
                setSearchableInfo(searchManager?.getSearchableInfo(componentName))
                setIconifiedByDefault(false) // Always show the search view
                setQueryWithoutFocus(viewModel.query)
            }
        }

        return true
    }

    private fun SearchView.setQueryWithoutFocus(query: String?) {
        // Force losing the focus to prevent the suggestions from appearing
        clearFocus()
        isFocusable = false
        isFocusableInTouchMode = false
        setQuery(query, false)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        // Search Intent sent by Google Now
        private const val GMS_ACTION_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION"
    }
}