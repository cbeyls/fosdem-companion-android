package be.digitalia.fosdem.api

import android.os.SystemClock
import androidx.annotation.MainThread
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.flowWhileShared
import be.digitalia.fosdem.flow.schedulerFlow
import be.digitalia.fosdem.flow.sharedFlow
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.LoadingState
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.parsers.EventsParser
import be.digitalia.fosdem.parsers.RoomStatusesParser
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.ByteCountSource
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.utils.network.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.buffer
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
@Singleton
class FosdemApi @Inject constructor(
    private val httpClient: HttpClient,
    private val scheduleDao: ScheduleDao,
    private val alarmManager: AppAlarmManager
) {
    private var downloadJob: Job? = null
    private val _downloadScheduleState =
        MutableStateFlow<LoadingState<DownloadScheduleResult>>(LoadingState.Idle())

    /**
     * Download & store the schedule to the database.
     * Only a single Job will be active at a time.
     * The result will be sent back through downloadScheduleResult LiveData.
     */
    @MainThread
    fun downloadSchedule(): Job {
        // Returns the download job in progress, if any
        return downloadJob ?: BackgroundWorkScope.launch {
            downloadScheduleInternal()
            downloadJob = null
        }.also {
            downloadJob = it
        }
    }

    private suspend fun downloadScheduleInternal() {
        _downloadScheduleState.value = LoadingState.Loading()
        val res = try {
            val response = httpClient.get(FosdemUrls.schedule, scheduleDao.lastModifiedTag.first()) { body, headers ->
                val length = body.contentLength()
                val source = if (length > 0L) {
                    // Broadcast the progression in percents, with a precision of 1/10 of the total file size
                    ByteCountSource(body.source(), length / 10L) { byteCount ->
                        // Cap percent to 100
                        val percent = (byteCount * 100L / length).toInt().coerceAtMost(100)
                        _downloadScheduleState.value = LoadingState.Loading(percent)
                    }.buffer()
                } else {
                    body.source()
                }

                val events = EventsParser().parse(source)
                scheduleDao.storeSchedule(events, headers.get(HttpClient.LAST_MODIFIED_HEADER_NAME))
            }
            when (response) {
                is HttpClient.Response.NotModified -> DownloadScheduleResult.UpToDate    // Nothing parsed, the result is up-to-date
                is HttpClient.Response.Success -> {
                    alarmManager.onScheduleRefreshed()
                    DownloadScheduleResult.Success(response.body)
                }
            }
        } catch (e: Exception) {
            DownloadScheduleResult.Error
        }
        _downloadScheduleState.value = LoadingState.Idle(res)
    }

    val downloadScheduleState: StateFlow<LoadingState<DownloadScheduleResult>> =
        _downloadScheduleState.asStateFlow()

    fun downloadScheduleResultConsumed() {
        _downloadScheduleState.update { state ->
            when (state) {
                is LoadingState.Loading -> state
                is LoadingState.Idle -> LoadingState.Idle()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomStatuses: Flow<Map<String, RoomStatus>> by lazy(LazyThreadSafetyMode.NONE) {
        sharedFlow(BackgroundWorkScope) { subscriptionCount ->
            // The room statuses will only be loaded when the event is live.
            // Use the days from the database to determine it.
            val scheduler = scheduleDao.days.flatMapLatest { days ->
                val startEndTimestamps = LongArray(days.size * 2)
                var index = 0
                for (day in days) {
                    startEndTimestamps[index++] = day.date.atTime(DAY_START_TIME)
                        .atZone(DateUtils.conferenceZoneId)
                        .toEpochSecond() * 1000L
                    startEndTimestamps[index++] = day.date.atTime(DAY_END_TIME)
                        .atZone(DateUtils.conferenceZoneId)
                        .toEpochSecond() * 1000L
                }
                schedulerFlow(startEndTimestamps)
                    .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed(5000L))
            }
            val offlineRoomStatuses = flowOf(emptyMap<String, RoomStatus>())
            scheduler.distinctUntilChanged().flatMapLatest { isLive ->
                if (isLive) {
                    buildLiveRoomStatusesFlow()
                        .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed(5000L))
                }
                else offlineRoomStatuses
            }
        }
        // Implementors: replace the above code block with the next line to disable room status support
        // emptyFlow()
    }

    /**
     * Builds a stateful cold Flow which loads and refreshes the Room statuses during the event.
     */
    private fun buildLiveRoomStatusesFlow(): Flow<Map<String, RoomStatus>> {
        var nextRefreshTime = 0L
        var expirationTime = Long.MAX_VALUE
        var retryAttempt = 0

        return flow {
            var now = SystemClock.elapsedRealtime()
            var nextRefreshDelay = nextRefreshTime - now

            if (now > expirationTime) {
                // When the data expires, replace it with an empty value
                emit(emptyMap())
            }

            while (true) {
                delay(nextRefreshDelay)

                nextRefreshDelay = try {
                    val response = httpClient.get(FosdemUrls.rooms) { body, _ ->
                        RoomStatusesParser().parse(body.source())
                    }
                    now = SystemClock.elapsedRealtime()

                    retryAttempt = 0
                    expirationTime = now + ROOM_STATUS_EXPIRATION_DELAY
                    emit(response.body)
                    ROOM_STATUS_REFRESH_DELAY
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }
                    now = SystemClock.elapsedRealtime()

                    if (now > expirationTime) {
                        emit(emptyMap())
                    }

                    // Use exponential backoff for retries
                    val multiplier = 2.0.pow(retryAttempt).toLong()
                    retryAttempt++
                    (ROOM_STATUS_FIRST_RETRY_DELAY * multiplier).coerceAtMost(ROOM_STATUS_REFRESH_DELAY)
                }

                nextRefreshTime = now + nextRefreshDelay
            }
        }
    }

    companion object {
        private val DAY_START_TIME = LocalTime.of(8, 30)
        private val DAY_END_TIME = LocalTime.of(19, 0)
        private val ROOM_STATUS_REFRESH_DELAY = TimeUnit.SECONDS.toMillis(90L)
        private val ROOM_STATUS_FIRST_RETRY_DELAY = TimeUnit.SECONDS.toMillis(30L)
        private val ROOM_STATUS_EXPIRATION_DELAY = TimeUnit.MINUTES.toMillis(6L)
    }
}