package be.digitalia.fosdem.activities

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.SearchResultListFragment
import be.digitalia.fosdem.utils.trimNonAlpha
import be.digitalia.fosdem.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample

@AndroidEntryPoint
class SearchResultActivity : AppCompatActivity(R.layout.search_result) {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        searchEditText = findViewById(R.id.search_edittext)
        val searchClearButton: View = findViewById(R.id.search_clear)

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        searchEditText.textChangeEvents
            .onEach {
                // immediately update the button state
                searchClearButton.isGone = it.isNullOrEmpty()
            }
            .sample(SEARCH_INPUT_SAMPLE_MILLIS)
            .onEach {
                // only update the results every SEARCH_INPUT_SAMPLE_MILLIS
                viewModel.query = it?.toString() ?: ""
            }
            .launchIn(lifecycleScope)

        searchClearButton.setOnClickListener {
            searchEditText.text = null
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit { add<SearchResultListFragment>(R.id.content) }
            handleIntent(intent)
            searchEditText.requestFocus()
        } else {
            searchEditText.setText(viewModel.query)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val query = when (intent.action) {
            Intent.ACTION_SEARCH, GMS_ACTION_SEARCH -> intent.getStringExtra(SearchManager.QUERY)
                ?.trimNonAlpha() ?: ""
            else -> ""
        }
        viewModel.query = query
        searchEditText.setText(query)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val EditText.textChangeEvents: Flow<CharSequence?>
        get() = callbackFlow {
            val textWatcher = doOnTextChanged { text, _, _, _ -> trySend(text) }
            awaitClose { removeTextChangedListener(textWatcher) }
        }

    companion object {
        // Search Intent sent by Google Now
        private const val GMS_ACTION_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION"
        private const val SEARCH_INPUT_SAMPLE_MILLIS = 400L
    }
}