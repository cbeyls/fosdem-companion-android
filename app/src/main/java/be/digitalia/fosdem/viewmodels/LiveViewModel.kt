package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val heartbeat = LiveDataFactory.interval(REFRESH_PERIOD)

    val nextEvents: LiveData<PagedList<StatusEvent>> = heartbeat.switchMap {
        val now = Instant.now()
        scheduleDao.getEventsWithStartTime(now, now + NEXT_EVENTS_INTERVAL).toLiveData(20)
    }

    val eventsInProgress: LiveData<PagedList<StatusEvent>> = heartbeat.switchMap {
        scheduleDao.getEventsInProgress(Instant.now()).toLiveData(20)
    }

    companion object {
        private val REFRESH_PERIOD = TimeUnit.MINUTES.toMillis(1L)
        private val NEXT_EVENTS_INTERVAL = Duration.ofMinutes(30L)
    }
}