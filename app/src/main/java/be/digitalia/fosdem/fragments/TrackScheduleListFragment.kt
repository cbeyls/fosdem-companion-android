package be.digitalia.fosdem.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.TrackScheduleAdapter
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.viewmodels.TrackScheduleViewModel

class TrackScheduleListFragment : RecyclerViewFragment(), TrackScheduleAdapter.EventClickListener {

    /**
     * Interface implemented by container activities
     */
    interface Callbacks {
        fun onEventSelected(position: Int, event: Event?)
    }

    private val viewModel: TrackScheduleViewModel by viewModels()
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        TrackScheduleAdapter(requireActivity(), this)
    }
    private var listener: Callbacks? = null
    private var selectionEnabled = false
    private var isListAlreadyShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectionEnabled = resources.getBoolean(R.bool.tablet_landscape)

        val args = requireArguments()
        val day: Day = args.getParcelable(ARG_DAY)!!
        val track: Track = args.getParcelable(ARG_TRACK)!!

        with(viewModel) {
            setDayAndTrack(day, track)
            currentTime.observe(this@TrackScheduleListFragment) { now ->
                adapter.currentTime = now
            }
        }

        if (savedInstanceState != null) {
            isListAlreadyShown = savedInstanceState.getBoolean(STATE_IS_LIST_ALREADY_SHOWN)
        }
        selectedId = savedInstanceState?.getLong(STATE_SELECTED_ID)
                ?: args.getLong(ARG_FROM_EVENT_ID, RecyclerView.NO_ID)
    }

    private var selectedId: Long = RecyclerView.NO_ID
        set(value) {
            field = value
            if (selectionEnabled) {
                adapter.selectedId = value
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_LIST_ALREADY_SHOWN, isListAlreadyShown)
        outState.putLong(STATE_SELECTED_ID, selectedId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Callbacks) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun notifyEventSelected(position: Int, event: Event?) {
        listener?.onEventSelected(position, event)
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) = with(recyclerView) {
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setAdapter(adapter)
        emptyText = getString(R.string.no_data)
        isProgressBarVisible = true

        viewModel.schedule.observe(viewLifecycleOwner) { schedule ->
            adapter.submitList(schedule)

            if (selectionEnabled) {
                var selectedPosition = adapter.getPositionForId(selectedId)
                if (selectedPosition == RecyclerView.NO_POSITION && adapter.itemCount > 0) {
                    // There is no current valid selection, reset to use the first item
                    selectedId = adapter.getItemId(0)
                    selectedPosition = 0
                }

                // Ensure the current selection is visible
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    recyclerView?.scrollToPosition(selectedPosition)
                }
                // Notify the parent of the current selection to synchronize its state
                notifyEventSelected(selectedPosition,
                        if (selectedPosition == RecyclerView.NO_POSITION) null else schedule[selectedPosition].event)

            } else if (!isListAlreadyShown) {
                val position = adapter.getPositionForId(selectedId)
                if (position != RecyclerView.NO_POSITION) {
                    recyclerView?.scrollToPosition(position)
                }
            }
            isListAlreadyShown = true

            isProgressBarVisible = false
        }
    }

    override fun onEventClick(position: Int, event: Event) {
        selectedId = event.id
        notifyEventSelected(position, event)
    }

    companion object {
        private const val ARG_DAY = "day"
        private const val ARG_TRACK = "track"
        private const val ARG_FROM_EVENT_ID = "from_event_id"

        private const val STATE_IS_LIST_ALREADY_SHOWN = "isListAlreadyShown"
        private const val STATE_SELECTED_ID = "selectedId"

        fun newInstance(day: Day, track: Track, fromEventId: Long = RecyclerView.NO_ID) = TrackScheduleListFragment().apply {
            arguments = Bundle(3).apply {
                putParcelable(ARG_DAY, day)
                putParcelable(ARG_TRACK, track)
                putLong(ARG_FROM_EVENT_ID, fromEventId)
            }
        }
    }
}