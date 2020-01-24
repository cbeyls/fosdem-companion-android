package be.digitalia.fosdem.activities

import android.content.Intent
import android.nfc.NdefRecord
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.EventDetailsFragment
import be.digitalia.fosdem.fragments.RoomImageDialogFragment
import be.digitalia.fosdem.fragments.TrackScheduleListFragment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.*
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel
import be.digitalia.fosdem.widgets.setupBookmarkStatus

/**
 * Track Schedule container, works in both single pane and dual pane modes.
 *
 * @author Christophe Beyls
 */
class TrackScheduleActivity : AppCompatActivity(), TrackScheduleListFragment.Callbacks, CreateNfcAppDataCallback {

    private val bookmarkStatusViewModel: BookmarkStatusViewModel by viewModels()
    private val day by lazy<Day>(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra(EXTRA_DAY)!!
    }
    private val track by lazy<Track>(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra(EXTRA_TRACK)!!
    }
    private var isTabletLandscape = false
    private var lastSelectedEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.track_schedule)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = track.toString()
            subtitle = day.toString()
        }
        title = "$track, $day"
        val trackType = track.type
        if (this.isLightTheme) {
            window.statusBarColorCompat = ContextCompat.getColor(this, trackType.statusBarColorResId)
            val trackAppBarColor = ContextCompat.getColorStateList(this, trackType.appBarColorResId)!!
            setTaskColorPrimary(trackAppBarColor.defaultColor)
            toolbar.tintBackground(trackAppBarColor)
        } else {
            val trackTextColor = ContextCompat.getColorStateList(this, trackType.textColorResId)!!
            toolbar.setTitleTextColor(trackTextColor)
        }

        isTabletLandscape = resources.getBoolean(R.bool.tablet_landscape)

        val fm = supportFragmentManager
        if (savedInstanceState == null) {
            val fromEventId = intent.getLongExtra(EXTRA_FROM_EVENT_ID, -1L)
            val trackScheduleListFragment = if (fromEventId != -1L) {
                TrackScheduleListFragment.newInstance(day, track, fromEventId)
            } else {
                TrackScheduleListFragment.newInstance(day, track)
            }
            fm.commit { add(R.id.schedule, trackScheduleListFragment) }
        } else {
            // Cleanup after switching from dual pane to single pane mode
            if (!isTabletLandscape) {
                val eventDetailsFragment = fm.findFragmentById(R.id.event)
                val roomImageDialogFragment = fm.findFragmentByTag(RoomImageDialogFragment.TAG)

                if (eventDetailsFragment != null || roomImageDialogFragment != null) {
                    fm.commit {
                        if (eventDetailsFragment != null) {
                            remove(eventDetailsFragment)
                        }
                        if (roomImageDialogFragment != null) {
                            remove(roomImageDialogFragment)
                        }
                    }
                }
            }
        }

        if (isTabletLandscape) {
            findViewById<ImageButton?>(R.id.fab)?.setupBookmarkStatus(bookmarkStatusViewModel, this)

            // Enable Android Beam
            setNfcAppDataPushMessageCallbackIfAvailable(this)
        }
    }

    override fun getSupportParentActivityIntent(): Intent? {
        return super.getSupportParentActivityIntent()?.apply {
            // Add FLAG_ACTIVITY_SINGLE_TOP to ensure the Main activity in the back stack is not re-created
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    // TrackScheduleListFragment.Callbacks

    override fun onEventSelected(position: Int, event: Event?) {
        if (isTabletLandscape) {
            // Tablet mode: Show event details in the right pane fragment
            lastSelectedEvent = event

            val fm = supportFragmentManager
            val currentFragment = fm.findFragmentById(R.id.event) as EventDetailsFragment?
            if (event != null) {
                // Only replace the fragment if the event is different
                if (currentFragment?.event != event) {
                    // Allow state loss since the event fragment will be synchronized with the list selection after activity re-creation
                    fm.commit(allowStateLoss = true) {
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        replace(R.id.event, EventDetailsFragment.newInstance(event))
                    }
                }
            } else {
                // Nothing is selected because the list is empty
                if (currentFragment != null) {
                    fm.commit(allowStateLoss = true) { remove(currentFragment) }
                }
            }

            bookmarkStatusViewModel.event = event
        } else {
            // Classic mode: Show event details in a new activity
            val intent = Intent(this, TrackScheduleEventActivity::class.java)
                    .putExtra(TrackScheduleEventActivity.EXTRA_DAY, day)
                    .putExtra(TrackScheduleEventActivity.EXTRA_TRACK, track)
                    .putExtra(TrackScheduleEventActivity.EXTRA_POSITION, position)
            startActivity(intent)
        }
    }

    // CreateNfcAppDataCallback

    override fun createNfcAppData(): NdefRecord? {
        return lastSelectedEvent?.toNfcAppData(this)
    }

    companion object {
        const val EXTRA_DAY = "day"
        const val EXTRA_TRACK = "track"
        // Optional extra used as a hint for up navigation from an event
        const val EXTRA_FROM_EVENT_ID = "from_event_id"
    }
}