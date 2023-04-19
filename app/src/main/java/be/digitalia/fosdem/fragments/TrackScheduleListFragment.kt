package be.digitalia.fosdem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.TrackScheduleEventActivity
import be.digitalia.fosdem.adapters.TrackScheduleAdapter
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.assistedViewModels
import be.digitalia.fosdem.utils.getParcelableCompat
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.TrackScheduleListViewModel
import be.digitalia.fosdem.viewmodels.TrackScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackScheduleListFragment : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider
    @Inject
    lateinit var viewModelFactory: TrackScheduleListViewModel.Factory
    private val viewModel: TrackScheduleListViewModel by assistedViewModels {
        val args = requireArguments()
        val day: Day = args.getParcelableCompat(ARG_DAY)!!
        val track: Track = args.getParcelableCompat(ARG_TRACK)!!
        viewModelFactory.create(day, track)
    }
    private val activityViewModel: TrackScheduleViewModel by activityViewModels()
    private var isListAlreadyShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            isListAlreadyShown = savedInstanceState.getBoolean(STATE_IS_LIST_ALREADY_SHOWN)
        }
        selectedId = savedInstanceState?.getLong(STATE_SELECTED_ID)
            ?: requireArguments().getLong(ARG_FROM_EVENT_ID, RecyclerView.NO_ID)
    }

    private var selectedId: Long = RecyclerView.NO_ID

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_LIST_ALREADY_SHOWN, isListAlreadyShown)
        outState.putLong(STATE_SELECTED_ID, selectedId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isSelectionEnabled = resources.getBoolean(R.bool.tablet_landscape)

        val adapter = TrackScheduleAdapter(view.context) { event ->
            selectedId = event.id
            activityViewModel.selectedEvent = event

            if (!isSelectionEnabled) {
                // Classic mode: Show event details in a new activity
                val intent = Intent(requireContext(), TrackScheduleEventActivity::class.java)
                    .putExtra(TrackScheduleEventActivity.EXTRA_DAY, event.day)
                    .putExtra(TrackScheduleEventActivity.EXTRA_TRACK, event.track)
                    .putExtra(TrackScheduleEventActivity.EXTRA_EVENT_ID, event.id)
                startActivity(intent)
            }
        }
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_data)
            isProgressBarVisible = true
        }

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            launch {
                userSettingsProvider.zoneId.collect { zoneId ->
                    adapter.zoneId = zoneId
                }
            }

            launch {
                viewModel.schedule.collect { schedule ->
                    adapter.submitList(schedule)

                    var selectedPosition = if (selectedId == -1L) -1 else schedule.indexOfFirst { it.event.id == selectedId }
                    if (selectedPosition == -1) {
                        // There is no current valid selection, reset to use the first item (if any)
                        if (schedule.isNotEmpty()) {
                            selectedPosition = 0
                            selectedId = schedule[0].event.id
                        } else {
                            selectedId = -1L
                        }
                    }

                    activityViewModel.selectedEvent =
                        if (selectedPosition == -1) null else schedule[selectedPosition].event

                    // Ensure the selection is visible
                    if ((isSelectionEnabled || !isListAlreadyShown) && selectedPosition != -1) {
                        holder.recyclerView.scrollToPosition(selectedPosition)
                    }
                    isListAlreadyShown = true

                    holder.isProgressBarVisible = false
                }
            }

            launch {
                viewModel.currentTime.collect { now ->
                    adapter.currentTime = now
                }
            }

            if (isSelectionEnabled) {
                launch {
                    activityViewModel.selectedEventFlow.collect { event ->
                        adapter.selectedId = event?.id ?: RecyclerView.NO_ID
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_DAY = "day"
        private const val ARG_TRACK = "track"
        private const val ARG_FROM_EVENT_ID = "from_event_id"

        private const val STATE_IS_LIST_ALREADY_SHOWN = "isListAlreadyShown"
        private const val STATE_SELECTED_ID = "selectedId"

        fun createArguments(day: Day, track: Track, fromEventId: Long = RecyclerView.NO_ID) = Bundle(3).apply {
            putParcelable(ARG_DAY, day)
            putParcelable(ARG_TRACK, track)
            putLong(ARG_FROM_EVENT_ID, fromEventId)
        }
    }
}