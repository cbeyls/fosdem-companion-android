package be.digitalia.fosdem.api

import android.content.Context
import android.os.AsyncTask
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.livedata.LiveDataFactory.scheduler
import be.digitalia.fosdem.livedata.SingleEvent
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.parsers.EventsParser
import be.digitalia.fosdem.parsers.RoomStatusesParser
import be.digitalia.fosdem.utils.network.HttpUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
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

    private val isLoading = AtomicBoolean()
    private val progress = MutableLiveData<Int>()
    private val result = MutableLiveData<SingleEvent<DownloadScheduleResult>>()
    private var roomStatuses: LiveData<Map<String, RoomStatus>>? = null

    /**
     * Download & store the schedule to the database.
     * Only one thread at a time will perform the actual action, the other ones will return immediately.
     * The result will be sent back in the consumable Result LiveData.
     */
    @MainThread
    fun downloadSchedule(context: Context) {
        // TODO use coroutines to remove the need for an AtomicBoolean
        if (!isLoading.compareAndSet(false, true)) { // If a download is already in progress, return immediately
            return
        }
        val appContext = context.applicationContext
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            downloadScheduleInternal(appContext)
            isLoading.set(false)
        }
    }

    @WorkerThread
    private fun downloadScheduleInternal(context: Context) {
        progress.postValue(-1)
        val res = try {
            val scheduleDao = AppDatabase.getInstance(context).scheduleDao
            val httpResponse = HttpUtils.get(FosdemUrls.schedule, scheduleDao.lastModifiedTag) { percent ->
                progress.postValue(percent)
            }
            when (httpResponse) {
                is HttpUtils.Response.NotModified -> {
                    // Nothing to parse, the result is up-to-date
                    DownloadScheduleResult.UpToDate
                }
                is HttpUtils.Response.Success -> {
                    httpResponse.source.use { source ->
                        val events = EventsParser().parse(source)
                        val count = scheduleDao.storeSchedule(events.asIterable(), httpResponse.lastModified)
                        DownloadScheduleResult.Success(count)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DownloadScheduleResult.Error
        } finally {
            progress.postValue(100)
        }
        result.postValue(SingleEvent(res))
    }

    /**
     * @return The current schedule download progress:
     * -1   : in progress, indeterminate
     * 0..99: progress value
     * 100  : download complete or inactive
     */
    val downloadScheduleProgress: LiveData<Int>
        get() = progress

    val downloadScheduleResult: LiveData<SingleEvent<DownloadScheduleResult>>
        get() = result

    @MainThread
    fun getRoomStatuses(context: Context): LiveData<Map<String, RoomStatus>> {
        var statuses = roomStatuses
        if (statuses == null) {
            // The room statuses will only be loaded when the event is live.
            // Use the days from the database to determine it.
            val daysLiveData = AppDatabase.getInstance(context).scheduleDao.days
            val scheduler = daysLiveData.switchMap { days: List<Day> ->
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
            statuses = scheduler.switchMap { isLive: Boolean -> if (isLive) liveRoomStatuses else offlineRoomStatuses }
            // Implementors: replace the above code with the next line to disable room status support
            // statuses = MutableLiveData()
            roomStatuses = statuses
        }
        return statuses
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
                    val result = withContext(Dispatchers.IO) {
                        HttpUtils.get(FosdemUrls.rooms).use { source ->
                            RoomStatusesParser().parse(source)
                        }
                    }
                    now = SystemClock.elapsedRealtime()

                    retryAttempt = 0
                    expirationTime = now + ROOM_STATUS_EXPIRATION_DELAY
                    emit(result)
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