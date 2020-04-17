package be.digitalia.fosdem.activities

import android.content.Intent
import android.nfc.NdefRecord
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.EventDetailsFragment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.*
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel
import be.digitalia.fosdem.viewmodels.TrackScheduleEventViewModel
import be.digitalia.fosdem.widgets.ContentLoadingProgressBar
import be.digitalia.fosdem.widgets.setupBookmarkStatus

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 *
 * @author Christophe Beyls
 */
class TrackScheduleEventActivity : AppCompatActivity(R.layout.track_schedule_event), CreateNfcAppDataCallback {

    private val bookmarkStatusViewModel: BookmarkStatusViewModel by viewModels()
    private val viewModel: TrackScheduleEventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.bottom_appbar))

        val intent = intent
        val day: Day = intent.getParcelableExtra(EXTRA_DAY)!!
        val track: Track = intent.getParcelableExtra(EXTRA_TRACK)!!

        val progress: ContentLoadingProgressBar = findViewById(R.id.progress)
        val pager: ViewPager2 = findViewById(R.id.pager)
        pager.recyclerView.enforceSingleScrollDirection()
        val adapter = TrackScheduleEventAdapter(this)

        val initialEventId = if (savedInstanceState == null) {
            intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        } else -1L

        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationIcon(R.drawable.abc_ic_ab_back_material)
            setNavigationContentDescription(R.string.abc_action_bar_up_description)
            setNavigationOnClickListener { onSupportNavigateUp() }
            title = track.toString()
            subtitle = day.toString()
        }
        title = "$track, $day"
        val trackType = track.type
        if (isLightTheme) {
            window.statusBarColorCompat = ContextCompat.getColor(this, trackType.statusBarColorResId)
            val trackAppBarColor = ContextCompat.getColorStateList(this, trackType.appBarColorResId)!!
            setTaskColorPrimary(trackAppBarColor.defaultColor)
            findViewById<View>(R.id.appbar).tintBackground(trackAppBarColor)
        } else {
            val trackTextColor = ContextCompat.getColorStateList(this, trackType.textColorResId)
            toolbar.setTitleTextColor(trackTextColor!!)
        }

        // Monitor the currently displayed event to update the bookmark status in FAB
        findViewById<ImageButton>(R.id.fab).setupBookmarkStatus(bookmarkStatusViewModel, this)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bookmarkStatusViewModel.event = adapter.events?.getOrNull(position)
            }
        })

        progress.show()

        with(viewModel) {
            setDayAndTrack(day, track)
            scheduleSnapshot.observe(this@TrackScheduleEventActivity) { events ->
                progress.hide()

                pager.isVisible = true
                adapter.events = events

                // Delay setting the adapter
                // to ensure the current position is restored properly
                if (pager.adapter == null) {
                    pager.adapter = adapter

                    if (initialEventId != -1L) {
                        val position = events.indexOfFirst { it.id == initialEventId }
                        if (position != -1) {
                            pager.setCurrentItem(position, false)
                        }
                    }

                    bookmarkStatusViewModel.event = adapter.events?.getOrNull(pager.currentItem)
                }
            }
        }

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

    override fun createNfcAppData(): NdefRecord? {
        return bookmarkStatusViewModel.event?.toNfcAppData(this)
    }

    class TrackScheduleEventAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        var events: List<Event>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = events?.size ?: 0

        override fun getItemId(position: Int) = events!![position].id

        override fun containsItem(itemId: Long): Boolean {
            return events?.any { it.id == itemId } ?: false
        }

        override fun createFragment(position: Int): Fragment {
            return EventDetailsFragment.newInstance(events!![position]).apply {
                // Workaround for duplicate menu items bug
                setMenuVisibility(false)
            }
        }
    }

    companion object {
        const val EXTRA_DAY = "day"
        const val EXTRA_TRACK = "track"
        const val EXTRA_EVENT_ID = "event_id"
    }
}