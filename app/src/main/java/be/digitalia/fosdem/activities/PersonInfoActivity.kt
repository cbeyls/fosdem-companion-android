package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.PersonInfoListFragment
import be.digitalia.fosdem.model.Person

class PersonInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_extended_title)
        setSupportActionBar(findViewById(R.id.toolbar))

        val person: Person = intent.getParcelableExtra(EXTRA_PERSON)!!

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = person.name

        if (savedInstanceState == null) {
            val f = PersonInfoListFragment.newInstance(person)
            supportFragmentManager.commit { add(R.id.content, f) }
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