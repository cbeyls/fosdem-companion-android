package be.digitalia.fosdem.activities

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.ExternalBookmarksListFragment
import be.digitalia.fosdem.utils.consumeHorizontalWindowInsetsAsPadding
import be.digitalia.fosdem.utils.rootView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExternalBookmarksActivity : AppCompatActivity(R.layout.content) {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        rootView.consumeHorizontalWindowInsetsAsPadding()
        setSupportActionBar(findViewById(R.id.toolbar))

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