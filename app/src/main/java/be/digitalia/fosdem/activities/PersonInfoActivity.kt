package be.digitalia.fosdem.activities

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import be.digitalia.fosdem.R
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.fragments.PersonInfoListFragment
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.configureColorSchemes
import be.digitalia.fosdem.utils.getParcelableExtraCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersonInfoActivity : AppCompatActivity(R.layout.person_info) {

    @Inject
    lateinit var scheduleDao: ScheduleDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        val person: Person = intent.getParcelableExtraCompat(EXTRA_PERSON)!!

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = person.name

        findViewById<View>(R.id.fab).setOnClickListener {
            openPersonDetails(person)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add<PersonInfoListFragment>(R.id.content,
                    args = PersonInfoListFragment.createArguments(person))
            }
        }
    }

    private fun openPersonDetails(person: Person) {
        val context = this
        lifecycleScope.launch {
            val baseUrl = scheduleDao.baseUrl.first() ?: return@launch
            person.getUrl(baseUrl)?.let { url ->
                withStarted {
                    try {
                        CustomTabsIntent.Builder()
                            .configureColorSchemes(context, R.color.light_color_primary)
                            .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                            .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                            .build()
                            .launchUrl(context, url.toUri())
                    } catch (_: ActivityNotFoundException) {
                    }
                }
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