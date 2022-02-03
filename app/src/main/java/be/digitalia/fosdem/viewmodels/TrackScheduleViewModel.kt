package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform

/**
 * ViewModel used for communication between TrackScheduleActivity and TrackScheduleListFragment
 */
class TrackScheduleViewModel : ViewModel() {

    private val eventSelection = MutableStateFlow<EventSelection?>(null)
    val selectedEventFlow: Flow<Event?> = eventSelection.transform { selection ->
        if (selection != null) emit(selection.event)
    }

    var selectedEvent: Event?
        get() = eventSelection.value?.event
        set(value) {
            eventSelection.value = EventSelection(value)
        }

    private data class EventSelection(val event: Event?)
}