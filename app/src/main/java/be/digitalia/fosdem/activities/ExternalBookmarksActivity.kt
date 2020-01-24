package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.ExternalBookmarksListFragment
import be.digitalia.fosdem.utils.extractNfcAppData
import be.digitalia.fosdem.utils.hasNfcAppData
import be.digitalia.fosdem.utils.toBookmarks

class ExternalBookmarksActivity : SimpleToolbarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val intent = intent
            val bookmarkIds = if (intent.hasNfcAppData()) {
                intent.extractNfcAppData().toBookmarks()
            } else null
            if (bookmarkIds == null) {
                // Invalid data format, exit
                finish()
                return
            }

            val f = ExternalBookmarksListFragment.newInstance(bookmarkIds)
            supportFragmentManager.commit { add(R.id.content, f) }
        }
    }
}