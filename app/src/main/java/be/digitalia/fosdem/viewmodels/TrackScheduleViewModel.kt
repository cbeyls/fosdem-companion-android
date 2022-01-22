package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel used for communication between TrackScheduleActivity and TrackScheduleListFragment
 */
class TrackScheduleViewModel : ViewModel() {

    private val _eventSelection = MutableStateFlow<EventSelection>(EventSelection.Unknown)
    val eventSelection: StateFlow<EventSelection> = _eventSelection.asStateFlow()

    fun setSelectEvent(event: Event) {
        _eventSelection.value = EventSelection.EventSelected(event)
    }

    fun clearSelection() {
        _eventSelection.value = EventSelection.NoSelection
    }

    sealed class EventSelection {
        object Unknown : EventSelection()
        object NoSelection : EventSelection()
        data class EventSelected(val event: Event) : EventSelection()
    }
}