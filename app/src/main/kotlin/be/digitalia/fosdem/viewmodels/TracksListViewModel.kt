package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.inject.CallbackViewModelAssistedFactory
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

@AssistedInject
class TracksListViewModel(
    @Assisted day: Day,
    scheduleDao: ScheduleDao,
) : ViewModel() {

    val tracks: Flow<List<Track>> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(scheduleDao.version) {
            scheduleDao.getTracks(day)
        }
    }.filterNotNull()

    @AssistedFactory
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModelAssistedFactory>())
    @ViewModelAssistedFactoryKey(TracksListViewModel::class)
    fun interface Factory : CallbackViewModelAssistedFactory {
        fun create(day: Day): TracksListViewModel
    }
}
