package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.schedulerFlow
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.tickerFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.inject.CallbackViewModelAssistedFactory
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@AssistedInject
class TrackScheduleListViewModel(
    @Assisted day: Day,
    @Assisted track: Track,
    scheduleDao: ScheduleDao,
    clock: Clock,
) : ViewModel() {

    val schedule: Flow<List<StatusEvent>> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(scheduleDao.bookmarksVersion) {
            scheduleDao.getEvents(day, track)
        }
    }.filterNotNull()

    /**
     * @return The current time during the target day, or null outside the target day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTime: Flow<Instant?> =
        // Auto refresh during the day passed as argument
        schedulerFlow(listOf(day.startTime, day.endTime), clock)
            .flatMapLatest { isOn ->
                if (isOn) {
                    tickerFlow(TIME_REFRESH_PERIOD).map { clock.now() }
                } else {
                    flowOf(null)
                }
            }

    @AssistedFactory
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModelAssistedFactory>())
    @ViewModelAssistedFactoryKey(TrackScheduleListViewModel::class)
    fun interface Factory : CallbackViewModelAssistedFactory {
        fun create(day: Day, track: Track): TrackScheduleListViewModel
    }

    companion object {
        private val TIME_REFRESH_PERIOD = 1.minutes
    }
}
