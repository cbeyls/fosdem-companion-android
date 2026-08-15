package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.model.Event
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ViewModel used for communication between TrackScheduleActivity and TrackScheduleListFragment
 */
class TrackScheduleViewModel : ViewModel() {
    val selectedEvent = MutableStateFlow<Event?>(null)
}
