package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.model.Event
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ViewModel used for communication between TrackScheduleActivity and TrackScheduleListFragment
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class TrackScheduleViewModel : ViewModel() {
    val selectedEvent = MutableStateFlow<Event?>(null)
}
