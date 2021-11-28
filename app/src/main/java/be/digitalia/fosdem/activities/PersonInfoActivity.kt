package be.digitalia.fosdem.activities

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.add
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.PersonInfoListFragment
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.viewmodels.PersonInfoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonInfoActivity : AppCompatActivity(R.layout.person_info) {

    private val viewModel: PersonInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        val person: Person = intent.getParcelableExtra(EXTRA_PERSON)!!
        viewModel.setPerson(person)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = person.name

        findViewById<View>(R.id.fab).setOnClickListener {
            // Look for the first non-placeholder event in the paged list
            val statusEvent = viewModel.events.value?.firstOrNull { it != null }
            if (statusEvent != null) {
                val url = person.getUrl(statusEvent.event.day.date.year)
                if (url != null) {
                    try {
                        CustomTabsIntent.Builder()
                                .configureToolbarColors(this, R.color.light_color_primary)
                                .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
                                .setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right)
                                .build()
                                .launchUrl(this, Uri.parse(url))
                    } catch (ignore: ActivityNotFoundException) {
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add<PersonInfoListFragment>(R.id.content)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_PERSON = "person"
    }
}