package be.digitalia.fosdem.api

import android.annotation.SuppressLint
import android.os.*
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import be.digitalia.fosdem.api.FosdemUrls.rooms
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.parsers.RoomStatusesParser
import be.digitalia.fosdem.utils.network.HttpUtils.get
import kotlin.math.pow

/**
 * Loads and maintain the Room statuses live during the event.
 */
internal class LiveRoomStatusesLiveData : LiveData<Map<String, RoomStatus>>() {

    private val handler = Handler(Looper.getMainLooper(), Handler.Callback { msg: Message ->
        when (msg.what) {
            EXPIRE_WHAT -> {
                expire()
                true
            }
            REFRESH_WHAT -> {
                refresh()
                true
            }
            else -> false
        }
    })
    private var expirationTime = Long.MAX_VALUE
    private var nextRefreshTime = 0L
    private var retryAttempt = 0
    // TODO replace with LiveData coroutines builder
    private var currentTask: AsyncTask<Unit, Nothing, Map<String, RoomStatus>?>? = null

    override fun onActive() {
        val now = SystemClock.elapsedRealtime()
        if (expirationTime != Long.MAX_VALUE) {
            if (now < expirationTime) {
                handler.sendEmptyMessageDelayed(EXPIRE_WHAT, expirationTime - now)
            } else {
                expire()
            }
        }
        if (now < nextRefreshTime) {
            handler.sendEmptyMessageDelayed(REFRESH_WHAT, nextRefreshTime - now)
        } else {
            refresh()
        }
    }

    override fun onInactive() {
        handler.removeMessages(EXPIRE_WHAT)
        handler.removeMessages(REFRESH_WHAT)
    }

    @SuppressLint("StaticFieldLeak")
    fun refresh() {
        if (currentTask != null) {
            // Let the ongoing task complete with success or error
            return
        }
        val task = object : AsyncTask<Unit, Nothing, Map<String, RoomStatus>?>() {

            override fun doInBackground(vararg params: Unit): Map<String, RoomStatus>? {
                return try {
                    get(rooms).use { source -> RoomStatusesParser().parse(source) }
                } catch (e: Throwable) {
                    null
                }
            }

            override fun onPostExecute(result: Map<String, RoomStatus>?) {
                currentTask = null
                if (result != null) {
                    onSuccess(result)
                } else {
                    onError()
                }
            }
        }
        currentTask = task
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun onSuccess(result: Map<String, RoomStatus>) {
        value = result
        retryAttempt = 0
        val now = SystemClock.elapsedRealtime()
        expirationTime = now + EXPIRATION_DELAY
        if (hasActiveObservers()) {
            handler.sendEmptyMessageDelayed(EXPIRE_WHAT, EXPIRATION_DELAY)
        }
        scheduleNextRefresh(now, REFRESH_DELAY)
    }

    private fun onError() {
        // Use exponential backoff for retries
        val multiplier = 2.0.pow(retryAttempt).toLong()
        retryAttempt++
        scheduleNextRefresh(SystemClock.elapsedRealtime(),
                (FIRST_ERROR_REFRESH_DELAY * multiplier).coerceAtMost(REFRESH_DELAY))
    }

    private fun scheduleNextRefresh(now: Long, delay: Long) {
        nextRefreshTime = now + delay
        if (hasActiveObservers()) {
            handler.sendEmptyMessageDelayed(REFRESH_WHAT, delay)
        }
    }

    private fun expire() {
        // When the data expires, replace it with an empty value
        value = emptyMap()
        expirationTime = Long.MAX_VALUE
    }

    companion object {
        private const val REFRESH_DELAY = 90L * DateUtils.SECOND_IN_MILLIS
        private const val FIRST_ERROR_REFRESH_DELAY = 30L * DateUtils.SECOND_IN_MILLIS
        private const val EXPIRATION_DELAY = 6L * DateUtils.MINUTE_IN_MILLIS
        private const val EXPIRE_WHAT = 0
        private const val REFRESH_WHAT = 1
    }
}