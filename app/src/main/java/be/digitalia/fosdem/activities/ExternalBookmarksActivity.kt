package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.fragment.app.add
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.ExternalBookmarksListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExternalBookmarksActivity : SimpleToolbarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val intent = intent
            val bookmarkIds = intent.getLongArrayExtra(EXTRA_BOOKMARK_IDS)
            if (bookmarkIds == null) {
                // Invalid data format, exit
                finish()
                return
            }

            supportFragmentManager.commit {
                add<ExternalBookmarksListFragment>(R.id.content,
                        args = ExternalBookmarksListFragment.createArguments(bookmarkIds))
            }
        }
    }

    companion object {
        const val EXTRA_BOOKMARK_IDS = "bookmark_ids"
    }
}