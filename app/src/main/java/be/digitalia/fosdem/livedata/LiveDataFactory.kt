package be.digitalia.fosdem.livedata

import android.os.Looper
import android.os.SystemClock
import androidx.core.os.HandlerCompat
import androidx.lifecycle.LiveData
import java.util.Arrays
import java.util.concurrent.TimeUnit

object LiveDataFactory {

    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    fun interval(period: Long, unit: TimeUnit): LiveData<Long> {
        return IntervalLiveData(unit.toMillis(period))
    }

    /**
     * Builds a LiveData whose value is true during scheduled periods.
     *
     * @param startEndTimestamps a list of timestamps in milliseconds, sorted in chronological order.
     * Odd and even values represent beginnings and ends of periods, respectively.
     */
    fun scheduler(vararg startEndTimestamps: Long): LiveData<Boolean> {
        return SchedulerLiveData(startEndTimestamps)
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

    private class SchedulerLiveData(private val startEndTimestamps: LongArray) : LiveData<Boolean>(), Runnable {

        private var nowPosition = -1

        override fun onActive() {
            val now = System.currentTimeMillis()
            updateState(now, Arrays.binarySearch(startEndTimestamps, now))
        }

        override fun onInactive() {
            handler.removeCallbacks(this)
        }

        override fun run() {
            val position = nowPosition
            updateState(startEndTimestamps[position], position)
        }

        private fun updateState(now: Long, position: Int) {
            var pos = position
            val size = startEndTimestamps.size
            if (pos >= 0) {
                do {
                    pos++
                } while (pos < size && startEndTimestamps[pos] == now)
            } else {
                pos = pos.inv()
            }
            val isOn = pos % 2 != 0
            if (value != isOn) {
                value = isOn
            }
            if (pos < size) {
                nowPosition = pos
                handler.postDelayed(this, startEndTimestamps[pos] - now)
            }
        }
    }
}