package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

@HiltViewModel(assistedFactory = TracksListViewModel.Factory::class)
class TracksListViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day
) : ViewModel() {

    val tracks: Flow<List<Track>> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(scheduleDao.version) {
            scheduleDao.getTracks(day)
        }
    }.filterNotNull()

    @AssistedFactory
    interface Factory {
        fun create(day: Day): TracksListViewModel
    }
}