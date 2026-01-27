package be.digitalia.fosdem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.settings.RoomColorManager
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.getParcelableCompat
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.BookmarksCalendarViewModel
import be.digitalia.fosdem.widgets.CalendarDayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class BookmarksCalendarDayFragment : Fragment(R.layout.fragment_bookmarks_calendar_day) {

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider

    @Inject
    lateinit var roomColorManager: RoomColorManager

    private val viewModel: BookmarksCalendarViewModel by viewModels({ requireParentFragment() })

    private val day: Day by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getParcelableCompat(ARG_DAY)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendarDayView: CalendarDayView = view.findViewById(R.id.calendar_day_view)

        // Apply window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
            )
            calendarDayView.setPadding(
                padding.left,
                calendarDayView.paddingTop,
                padding.right,
                padding.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        calendarDayView.roomColorProvider = roomColorManager::getColorForRoom
        calendarDayView.onEventClickListener = { event ->
            launchEventDetails(event)
        }

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            launch {
                userSettingsProvider.timeZoneMode.collect { mode ->
                    calendarDayView.timeZoneOverride = mode.override
                }
            }
            launch {
                viewModel.bookmarksByDay.filterNotNull().collect { bookmarksByDay ->
                    val dayBookmarks = bookmarksByDay[day.index] ?: emptyList()
                    calendarDayView.events = dayBookmarks
                }
            }
            launch {
                while (true) {
                    calendarDayView.currentTime = Instant.now()
                    delay(60_000L)
                }
            }
        }
    }

    private fun launchEventDetails(event: Event) {
        val intent = Intent(requireContext(), EventDetailsActivity::class.java)
            .setData("${event.id}".toUri())
            .putExtra(EventDetailsActivity.EXTRA_EVENT, event)
        startActivity(intent)
    }

    companion object {
        private const val ARG_DAY = "day"

        fun createArguments(day: Day) = Bundle(1).apply {
            putParcelable(ARG_DAY, day)
        }
    }
}
