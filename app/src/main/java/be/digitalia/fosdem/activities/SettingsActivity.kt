package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.SettingsFragment
import be.digitalia.fosdem.utils.ActivityTransitionOverrideType
import be.digitalia.fosdem.utils.consumeHorizontalWindowInsetsAsPadding
import be.digitalia.fosdem.utils.overrideActivityTransitionCompat
import be.digitalia.fosdem.utils.rootView
import be.digitalia.fosdem.utils.setupEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(R.layout.content) {

    override fun onCreate(savedInstanceState: Bundle?) {
        setupEdgeToEdge()
        super.onCreate(savedInstanceState)
        rootView.consumeHorizontalWindowInsetsAsPadding()
        overrideActivityTransitionCompat(
            overrideType = ActivityTransitionOverrideType.OPEN,
            enterAnim = R.anim.slide_in_right,
            exitAnim = R.anim.partial_zoom_out
        )
        overrideActivityTransitionCompat(
            overrideType = ActivityTransitionOverrideType.CLOSE,
            enterAnim = R.anim.partial_zoom_in,
            exitAnim = R.anim.slide_out_right
        )
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commit { add(R.id.content, SettingsFragment()) }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}