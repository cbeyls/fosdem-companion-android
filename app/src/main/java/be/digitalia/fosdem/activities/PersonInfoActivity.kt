package be.digitalia.fosdem.activities

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.PersonInfoListFragment
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.consumeHorizontalWindowInsetsAsPadding
import be.digitalia.fosdem.utils.getParcelableExtraCompat
import be.digitalia.fosdem.utils.rootView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonInfoActivity : AppCompatActivity(R.layout.person_info), FabOwner {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        rootView.consumeHorizontalWindowInsetsAsPadding()
        setSupportActionBar(findViewById(R.id.toolbar))

        val person: Person = intent.getParcelableExtraCompat(EXTRA_PERSON)!!

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = person.name

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add<PersonInfoListFragment>(R.id.content,
                    args = PersonInfoListFragment.createArguments(person))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override val fab: FloatingActionButton
        get() = findViewById(R.id.fab)

    companion object {
        const val EXTRA_PERSON = "person"
    }
}