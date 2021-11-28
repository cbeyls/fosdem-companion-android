package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TrackScheduleListViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val dayTrackLiveData = MutableLiveData<Pair<Day, Track>>()

    val schedule: LiveData<List<StatusEvent>> = dayTrackLiveData.switchMap { (day, track) ->
        scheduleDao.getEvents(day, track)
    }

    /**
     * @return The current time during the target day, or null outside of the target day.
     */
    val currentTime: LiveData<Instant?> = dayTrackLiveData
        .switchMap { (day, _) ->
            // Auto refresh during the day passed as argument
            val dayStart = day.date.atStartOfDay(DateUtils.conferenceZoneId).toInstant()
            LiveDataFactory.scheduler(
                dayStart.toEpochMilli(),
                (dayStart + Duration.ofDays(1L)).toEpochMilli()
            )
        }
        .switchMap { isOn ->
            if (isOn) {
                LiveDataFactory.interval(REFRESH_TIME_INTERVAL_MINUTES, TimeUnit.MINUTES).map {
                    Instant.now()
                }
            } else {
                MutableLiveData(null)
            }
        }

    fun setDayAndTrack(day: Day, track: Track) {
        val dayTrack = day to track
        if (dayTrack != dayTrackLiveData.value) {
            dayTrackLiveData.value = dayTrack
        }
    }

    companion object {
        private const val REFRESH_TIME_INTERVAL_MINUTES = 1L
    }
}