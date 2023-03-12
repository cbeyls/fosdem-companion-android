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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.EventDetailsFragment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.CreateNfcAppDataCallback
import be.digitalia.fosdem.utils.assistedViewModels
import be.digitalia.fosdem.utils.enforceSingleScrollDirection
import be.digitalia.fosdem.utils.instantiate
import be.digitalia.fosdem.utils.isLightTheme
import be.digitalia.fosdem.utils.recyclerView
import be.digitalia.fosdem.utils.setNfcAppDataPushMessageCallbackIfAvailable
import be.digitalia.fosdem.utils.setTaskColorPrimary
import be.digitalia.fosdem.utils.statusBarColorCompat
import be.digitalia.fosdem.utils.tintBackground
import be.digitalia.fosdem.utils.toNfcAppData
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel
import be.digitalia.fosdem.viewmodels.TrackScheduleEventViewModel
import be.digitalia.fosdem.widgets.ContentLoadingViewMediator
import be.digitalia.fosdem.widgets.setupBookmarkStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class TrackScheduleEventActivity : AppCompatActivity(R.layout.track_schedule_event), CreateNfcAppDataCallback {

    private val bookmarkStatusViewModel: BookmarkStatusViewModel by viewModels()
    @Inject
    lateinit var viewModelFactory: TrackScheduleEventViewModel.Factory
    private val viewModel: TrackScheduleEventViewModel by assistedViewModels {
        viewModelFactory.create(day, track)
    }

    private val day: Day by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra(EXTRA_DAY)!!
    }
    private val track: Track by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra(EXTRA_TRACK)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.bottom_appbar))

        val progress = ContentLoadingViewMediator(findViewById(R.id.progress))
        val pager: ViewPager2 = findViewById(R.id.pager)
        pager.recyclerView.enforceSingleScrollDirection()
        val adapter = TrackScheduleEventAdapter(this)

        val initialEventId = if (savedInstanceState == null) {
            intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        } else -1L

        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
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
            val trackTextColor = ContextCompat.getColorStateList(this, trackType.textColorResId)!!
            toolbar.setTitleTextColor(trackTextColor)
        }

        // Monitor the currently displayed event to update the bookmark status in FAB
        findViewById<ImageButton>(R.id.fab).setupBookmarkStatus(bookmarkStatusViewModel, this)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bookmarkStatusViewModel.event = adapter.events.getOrNull(position)
            }
        })

        progress.isVisible = true

        lifecycleScope.launch {
            val events = viewModel.scheduleSnapshot.await()
            withStarted {
                progress.isVisible = false

                pager.isVisible = true
                adapter.events = events

                // Delay setting the adapter to ensure the current position is restored properly
                if (pager.adapter == null) {
                    pager.adapter = adapter

                    if (initialEventId != -1L) {
                        val position = events.indexOfFirst { it.id == initialEventId }
                        if (position != -1) {
                            pager.setCurrentItem(position, false)
                        }
                    }

                    bookmarkStatusViewModel.event = adapter.events.getOrNull(pager.currentItem)
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
        private val fragmentFactory = fragmentActivity.supportFragmentManager.fragmentFactory

        var events: List<Event> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = events.size

        override fun getItemId(position: Int) = events[position].id

        override fun containsItem(itemId: Long): Boolean {
            return events.any { it.id == itemId }
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentFactory.instantiate<EventDetailsFragment>().apply {
                arguments = EventDetailsFragment.createArguments(events[position])
            }
        }
    }

    companion object {
        const val EXTRA_DAY = "day"
        const val EXTRA_TRACK = "track"
        const val EXTRA_EVENT_ID = "event_id"
    }
}