package be.digitalia.fosdem.api

import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.livedata.LiveDataFactory.scheduler
import be.digitalia.fosdem.livedata.SingleEvent
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.parsers.EventsParser
import be.digitalia.fosdem.parsers.RoomStatusesParser
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.ByteCountSource
import be.digitalia.fosdem.utils.network.HttpUtils
import be.digitalia.fosdem.utils.network.HttpUtils.lastModified
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.buffer
import kotlin.math.pow

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
object FosdemApi {
    // 8:30 (local time)
    private const val DAY_START_TIME = 8 * DateUtils.HOUR_IN_MILLIS + 30 * DateUtils.MINUTE_IN_MILLIS
    // 19:00 (local time)
    private const val DAY_END_TIME = 19 * DateUtils.HOUR_IN_MILLIS
    private const val ROOM_STATUS_REFRESH_DELAY = 90L * DateUtils.SECOND_IN_MILLIS
    private const val ROOM_STATUS_FIRST_RETRY_DELAY = 30L * DateUtils.SECOND_IN_MILLIS
    private const val ROOM_STATUS_EXPIRATION_DELAY = 6L * DateUtils.MINUTE_IN_MILLIS

    private var downloadJob: Job? = null
    private val _downloadScheduleProgress = MutableLiveData<Int>()
    private val _downloadScheduleResult = MutableLiveData<SingleEvent<DownloadScheduleResult>>()
    private var roomStatuses: LiveData<Map<String, RoomStatus>>? = null

    /**
     * Download & store the schedule to the database.
     * Only a single Job will be active at a time.
     * The result will be sent back through downloadScheduleResult LiveData.
     */
    @MainThread
    fun downloadSchedule(context: Context): Job {
        // Returns the download job in progress, if any
        return downloadJob ?: run {
            val appContext = context.applicationContext
            BackgroundWorkScope.launch {
                downloadScheduleInternal(appContext)
                downloadJob = null
            }.also {
                downloadJob = it
            }
        }
    }

    @MainThread
    private suspend fun downloadScheduleInternal(context: Context) {
        _downloadScheduleProgress.value = -1
        val res = try {
            val scheduleDao = AppDatabase.getInstance(context).scheduleDao
            val response = HttpUtils.get(FosdemUrls.schedule, scheduleDao.lastModifiedTag) { body, rawResponse ->
                val length = body.contentLength()
                val source = if (length > 0L) {
                    // Broadcast the progression in percents, with a precision of 1/10 of the total file size
                    ByteCountSource(body.source(), length / 10L) { byteCount ->
                        // Cap percent to 100
                        val percent = (byteCount * 100L / length).toInt().coerceAtMost(100)
                        _downloadScheduleProgress.postValue(percent)
                    }.buffer()
                } else {
                    body.source()
                }

                val events = EventsParser().parse(source)
                scheduleDao.storeSchedule(events, rawResponse.lastModified)
            }
            when (response) {
                is HttpUtils.Response.NotModified -> DownloadScheduleResult.UpToDate    // Nothing to parse, the result is up-to-date
                is HttpUtils.Response.Success -> DownloadScheduleResult.Success(response.body)
            }
        } catch (e: Exception) {
            DownloadScheduleResult.Error
        }
        _downloadScheduleProgress.value = 100

        if (res is DownloadScheduleResult.Success) {
            FosdemAlarmManager.onScheduleRefreshed()
        }
        _downloadScheduleResult.value = SingleEvent(res)
    }

    /**
     * @return The current schedule download progress:
     * -1   : in progress, indeterminate
     * 0..99: progress value
     * 100  : download complete or inactive
     */
    val downloadScheduleProgress: LiveData<Int>
        get() = _downloadScheduleProgress

    val downloadScheduleResult: LiveData<SingleEvent<DownloadScheduleResult>>
        get() = _downloadScheduleResult

    @MainThread
    fun getRoomStatuses(context: Context): LiveData<Map<String, RoomStatus>> {
        return roomStatuses ?: run {
            // The room statuses will only be loaded when the event is live.
            // Use the days from the database to determine it.
            val scheduler = AppDatabase.getInstance(context).scheduleDao.days.switchMap { days ->
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
                    .also { roomStatuses = it }
            // Implementors: replace the above code with the next line to disable room status support
            // MutableLiveData().also { roomStatuses = it }
        }
    }

    /**
     * Builds a LiveData instance which loads and refreshes the Room statuses during the event.
     */
    private fun buildLiveRoomStatusesLiveData(): LiveData<Map<String, RoomStatus>> {
        var nextRefreshTime = 0L
        var expirationTime = Long.MAX_VALUE
        var retryAttempt = 0

        return liveData<Map<String, RoomStatus>> {
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
                    val response = HttpUtils.get(FosdemUrls.rooms) { body, _ ->
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
}