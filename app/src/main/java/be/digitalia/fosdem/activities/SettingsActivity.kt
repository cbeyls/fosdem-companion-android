package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.SettingsFragment
import be.digitalia.fosdem.utils.ActivityUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : SimpleToolbarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityUtils.overrideActivityTransition(
            this,
            ActivityUtils.OVERRIDE_TRANSITION_OPEN,
            R.anim.slide_in_right,
            R.anim.partial_zoom_out
        )
        ActivityUtils.overrideActivityTransition(
            this,
            ActivityUtils.OVERRIDE_TRANSITION_CLOSE,
            R.anim.partial_zoom_in,
            R.anim.slide_out_right
        )

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