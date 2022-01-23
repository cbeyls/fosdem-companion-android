package be.digitalia.fosdem.livedata

import android.os.Looper
import android.os.SystemClock
import androidx.core.os.HandlerCompat
import androidx.lifecycle.LiveData

object LiveDataFactory {

    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    fun interval(periodInMillis: Long): LiveData<Long> {
        return IntervalLiveData(periodInMillis)
    }

    private class IntervalLiveData(private val periodInMillis: Long) : LiveData<Long>(), Runnable {

        private var updateTime = 0L
        private var version = 0L

        override fun onActive() {
            val now = SystemClock.elapsedRealtime()
            if (now >= updateTime) {
                update(now)
            } else {
                handler.postDelayed(this, updateTime - now)
            }
        }

        override fun onInactive() {
            handler.removeCallbacks(this)
        }

        private fun update(now: Long) {
            value = version++
            updateTime = now + periodInMillis
            handler.postDelayed(this, periodInMillis)
        }

        override fun run() {
            update(SystemClock.elapsedRealtime())
        }
    }
}