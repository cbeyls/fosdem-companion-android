package be.digitalia.fosdem.activities

import android.content.Intent
import android.nfc.NdefRecord
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.observe
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.EventDetailsFragment
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.*
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel
import be.digitalia.fosdem.viewmodels.EventViewModel
import be.digitalia.fosdem.widgets.setupBookmarkStatus

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 *
 * @author Christophe Beyls
 */
class EventDetailsActivity : AppCompatActivity(R.layout.single_event), CreateNfcAppDataCallback {

    private val bookmarkStatusViewModel: BookmarkStatusViewModel by viewModels()
    private val viewModel: EventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.bottom_appbar))

        findViewById<ImageButton>(R.id.fab).setupBookmarkStatus(bookmarkStatusViewModel, this)

        val intentEvent: Event? = intent.getParcelableExtra(EXTRA_EVENT)

        if (intentEvent != null) {
            // The event has been passed as parameter, it can be displayed immediately
            initEvent(intentEvent)
            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    add<EventDetailsFragment>(R.id.content,
                            args = EventDetailsFragment.createArguments(intentEvent))
                }
            }
        } else {
            // Load the event from the DB using its id
            if (!viewModel.isEventIdSet) {
                val intent = intent
                val eventIdString = if (intent.hasNfcAppData()) {
                    // NFC intent
                    intent.extractNfcAppData().toEventIdString()
                } else {
                    // Normal in-app intent
                    intent.dataString!!
                }
                viewModel.setEventId(eventIdString.toLong())
            }

            viewModel.event.observe(this) { event ->
                if (event == null) {
                    // Event not found, quit
                    Toast.makeText(this, getString(R.string.event_not_found_error), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    initEvent(event)

                    val fm = supportFragmentManager
                    if (fm.findFragmentById(R.id.content) == null) {
                        fm.commit(allowStateLoss = true) {
                            add<EventDetailsFragment>(R.id.content,
                                    args = EventDetailsFragment.createArguments(event))
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize event-related configuration after the event has been loaded.
     */
    private fun initEvent(event: Event) {
        // Enable up navigation only after getting the event details
        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationIcon(R.drawable.abc_ic_ab_back_material)
            setNavigationContentDescription(R.string.abc_action_bar_up_description)
            setNavigationOnClickListener { onSupportNavigateUp() }
            title = event.track.name
        }

        val trackType = event.track.type
        if (isLightTheme) {
            window.statusBarColorCompat = ContextCompat.getColor(this, trackType.statusBarColorResId)
            val trackAppBarColor = ContextCompat.getColorStateList(this, trackType.appBarColorResId)!!
            setTaskColorPrimary(trackAppBarColor.defaultColor)
            findViewById<View>(R.id.appbar).tintBackground(trackAppBarColor)
        } else {
            val trackTextColor = ContextCompat.getColorStateList(this, trackType.textColorResId)!!
            toolbar.setTitleTextColor(trackTextColor)
        }

        bookmarkStatusViewModel.event = event

        // Enable Android Beam
        setNfcAppDataPushMessageCallbackIfAvailable(this)
    }

    override fun getSupportParentActivityIntent(): Intent? {
        val event = bookmarkStatusViewModel.event ?: return null
        // Navigate up to the track associated with this event
        return Intent(this, TrackScheduleActivity::class.java)
                .putExtra(TrackScheduleActivity.EXTRA_DAY, event.day)
                .putExtra(TrackScheduleActivity.EXTRA_TRACK, event.track)
                .putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.id)
    }

    override fun supportNavigateUpTo(upIntent: Intent) {
        // Replicate the compatibility implementation of NavUtils.navigateUpTo()
        // to ensure the parent Activity is always launched
        // even if not present on the back stack.
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(upIntent)
        finish()
    }

    // CreateNfcAppDataCallback

    override fun createNfcAppData(): NdefRecord? {
        return bookmarkStatusViewModel.event?.toNfcAppData(this)
    }

    companion object {
        const val EXTRA_EVENT = "event"
    }
}