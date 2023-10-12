package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.countSubscriptionsFlow
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.synchronizedTickerFlow
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class LiveViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    // Share a single ticker providing the time to ensure both lists are synchronized
    private val ticker: Flow<Instant> = stateFlow(viewModelScope, null) {
        synchronizedTickerFlow(REFRESH_PERIOD)
            .map { Instant.now() }
    }.filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createLiveEventsHotFlow(
        pagingSourceFactory: (now: Instant) -> PagingSource<Int, StatusEvent>
    ): Flow<PagingData<StatusEvent>> {
        return countSubscriptionsFlow {
            ticker
                .flowWhileShared(SharingStarted.WhileSubscribed())
                .distinctUntilChanged()
                .flatMapLatest { now ->
                    Pager(PagingConfig(20)) { pagingSourceFactory(now) }.flow
                }.cachedIn(viewModelScope)
        }
    }

    val nextEvents: Flow<PagingData<StatusEvent>> = createLiveEventsHotFlow { now ->
        scheduleDao.getEventsWithStartTime(now, now + NEXT_EVENTS_INTERVAL)
    }

    val eventsInProgress: Flow<PagingData<StatusEvent>> = createLiveEventsHotFlow { now ->
        scheduleDao.getEventsInProgress(now)
    }

    companion object {
        private val REFRESH_PERIOD = 1.minutes
        private val NEXT_EVENTS_INTERVAL = Duration.ofMinutes(30L)
    }
}