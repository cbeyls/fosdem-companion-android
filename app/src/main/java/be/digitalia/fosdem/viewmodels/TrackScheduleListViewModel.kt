package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.schedulerFlow
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.tickerFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import be.digitalia.fosdem.utils.AppTimeSource
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@HiltViewModel(assistedFactory = TrackScheduleListViewModel.Factory::class)
class TrackScheduleListViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day,
    @Assisted track: Track
) : ViewModel() {

    val schedule: Flow<List<StatusEvent>> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(scheduleDao.bookmarksVersion) {
            scheduleDao.getEvents(day, track)
        }
    }.filterNotNull()

    /**
     * @return The current time during the target day, or null outside of the target day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTime: Flow<Instant?> =
        // Restart when debug clock offset changes
        AppTimeSource.offsetFlow.flatMapLatest {
            // Auto refresh during the day passed as argument
            schedulerFlow(
                day.startTime.toEpochMilli(), day.endTime.toEpochMilli()
            ).flatMapLatest { isOn ->
                if (isOn) {
                    tickerFlow(TIME_REFRESH_PERIOD).map { AppTimeSource.now() }
                } else {
                    flowOf(null)
                }
            }
        }

    @AssistedFactory
    interface Factory {
        fun create(day: Day, track: Track): TrackScheduleListViewModel
    }

    companion object {
        private val TIME_REFRESH_PERIOD = 1.minutes
    }
}