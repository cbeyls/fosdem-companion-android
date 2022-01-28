package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.db.observableQuery
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class TracksListViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day
) : ViewModel() {

    val tracks: Flow<List<Track>> = stateFlow(viewModelScope, null) { subscriptionCount ->
        observableQuery(scheduleDao.version, subscriptionCount) {
            scheduleDao.getTracks(day)
        }
    }.filterNotNull()

    @AssistedFactory
    interface Factory {
        fun create(day: Day): TracksListViewModel
    }
}