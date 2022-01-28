package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.schedulerFlow
import be.digitalia.fosdem.flow.tickerFlow
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class TrackScheduleListViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day,
    @Assisted track: Track
) : ViewModel() {

    val schedule: LiveData<List<StatusEvent>> = scheduleDao.getEvents(day, track)

    /**
     * @return The current time during the target day, or null outside of the target day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTime: Flow<Instant?> = run {
        // Auto refresh during the day passed as argument
        val dayStart = day.date.atStartOfDay(DateUtils.conferenceZoneId).toInstant()
        schedulerFlow(
            dayStart.toEpochMilli(),
            (dayStart + Duration.ofDays(1L)).toEpochMilli()
        )
    }.flatMapLatest { isOn ->
        if (isOn) {
            tickerFlow(TIME_REFRESH_PERIOD).map { Instant.now() }
        } else {
            flowOf(null)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(day: Day, track: Track): TrackScheduleListViewModel
    }

    companion object {
        private val TIME_REFRESH_PERIOD = TimeUnit.MINUTES.toMillis(1L)
    }
}