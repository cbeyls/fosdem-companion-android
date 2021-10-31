package be.digitalia.fosdem.api

import android.os.SystemClock
import android.text.format.DateUtils
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.livedata.LiveDataFactory.scheduler
import be.digitalia.fosdem.livedata.SingleEvent
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.LoadingState
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.parsers.EventsParser
import be.digitalia.fosdem.parsers.RoomStatusesParser
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.ByteCountSource
import be.digitalia.fosdem.utils.network.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.buffer
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
    private val _downloadScheduleState = MutableLiveData<LoadingState<DownloadScheduleResult>>()

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

    @MainThread
    private suspend fun downloadScheduleInternal() {
        _downloadScheduleState.value = LoadingState.Loading()
        val res = try {
            val response = httpClient.get(FosdemUrls.schedule, scheduleDao.lastModifiedTag) { body, headers ->
                val length = body.contentLength()
                val source = if (length > 0L) {
                    // Broadcast the progression in percents, with a precision of 1/10 of the total file size
                    ByteCountSource(body.source(), length / 10L) { byteCount ->
                        // Cap percent to 100
                        val percent = (byteCount * 100L / length).toInt().coerceAtMost(100)
                        _downloadScheduleState.postValue(LoadingState.Loading(percent))
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
        _downloadScheduleState.value = LoadingState.Idle(SingleEvent(res))
    }

    val downloadScheduleState: LiveData<LoadingState<DownloadScheduleResult>>
        get() = _downloadScheduleState

    val roomStatuses: LiveData<Map<String, RoomStatus>> by lazy(LazyThreadSafetyMode.NONE) {
        // The room statuses will only be loaded when the event is live.
        // Use the days from the database to determine it.
        val scheduler = scheduleDao.days.switchMap { days ->
            val startEndTimestamps = LongArray(days.size * 2)
            var index = 0
            for (day in days) {
                val dayStart = day.date.time
                startEndTimestamps[index++] = dayStart + DAY_START_TIME
                startEndTimestamps[index++] = dayStart + DAY_END_TIME
            }
            scheduler(*startEndTimestamps)
        }
        val liveRoomStatuses = buildLiveRoomStatusesLiveData()
        val offlineRoomStatuses = MutableLiveData(emptyMap<String, RoomStatus>())
        scheduler.switchMap { isLive -> if (isLive) liveRoomStatuses else offlineRoomStatuses }
        // Implementors: replace the above code block with the next line to disable room status support
        // MutableLiveData()
    }

    /**
     * Builds a LiveData instance which loads and refreshes the Room statuses during the event.
     */
    private fun buildLiveRoomStatusesLiveData(): LiveData<Map<String, RoomStatus>> {
        var nextRefreshTime = 0L
        var expirationTime = Long.MAX_VALUE
        var retryAttempt = 0

        return liveData {
            var now = SystemClock.elapsedRealtime()
            var nextRefreshDelay = nextRefreshTime - now

            if (now > expirationTime && latestValue?.isEmpty() == false) {
                // When the data expires, replace it with an empty value
                emit(emptyMap())
            }

            while (true) {
                if (nextRefreshDelay > 0) {
                    delay(nextRefreshDelay)
                }

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

                    if (now > expirationTime && latestValue?.isEmpty() == false) {
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
        // 8:30 (local time)
        private const val DAY_START_TIME = 8 * DateUtils.HOUR_IN_MILLIS + 30 * DateUtils.MINUTE_IN_MILLIS

        // 19:00 (local time)
        private const val DAY_END_TIME = 19 * DateUtils.HOUR_IN_MILLIS
        private const val ROOM_STATUS_REFRESH_DELAY = 90L * DateUtils.SECOND_IN_MILLIS
        private const val ROOM_STATUS_FIRST_RETRY_DELAY = 30L * DateUtils.SECOND_IN_MILLIS
        private const val ROOM_STATUS_EXPIRATION_DELAY = 6L * DateUtils.MINUTE_IN_MILLIS
    }
}