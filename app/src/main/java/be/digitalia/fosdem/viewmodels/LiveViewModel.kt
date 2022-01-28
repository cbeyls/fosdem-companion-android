package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.whileSubscribedTickerFlow
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.withIndex
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    // Share a single ticker to ensure both lists are updated at the same time
    private val ticker: Flow<IndexedValue<Unit>> =
        stateFlow(viewModelScope, IndexedValue(0, Unit)) { subscriptionCount ->
            // StateFlow will deduplicate the first emitted value (0)
            whileSubscribedTickerFlow(REFRESH_PERIOD, subscriptionCount).withIndex()
        }

    // TODO remove after migrating to Paging 3
    private val tickerLiveData = ticker.asLiveData(viewModelScope.coroutineContext, 0L)
        .distinctUntilChanged()

    val nextEvents: LiveData<PagedList<StatusEvent>> = tickerLiveData.switchMap {
        val now = Instant.now()
        scheduleDao.getEventsWithStartTime(now, now + NEXT_EVENTS_INTERVAL).toLiveData(20)
    }

    val eventsInProgress: LiveData<PagedList<StatusEvent>> = tickerLiveData.switchMap {
        scheduleDao.getEventsInProgress(Instant.now()).toLiveData(20)
    }

    companion object {
        private val REFRESH_PERIOD = TimeUnit.MINUTES.toMillis(1L)
        private val NEXT_EVENTS_INTERVAL = Duration.ofMinutes(30L)
    }
}