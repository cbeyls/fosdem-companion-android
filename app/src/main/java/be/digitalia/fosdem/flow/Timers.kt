package be.digitalia.fosdem.flow

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Arrays

fun rememberIntervalFlow(periodInMillis: Long): Flow<Long> {
    var updateTime = 0L
    var version = 0L
    return flow {
        delay(updateTime - SystemClock.elapsedRealtime())
        while (true) {
            emit(version++)
            updateTime = SystemClock.elapsedRealtime() + periodInMillis
            delay(periodInMillis)
        }
    }
}

/**
 * Builds a Flow whose value is true during scheduled periods.
 *
 * @param startEndTimestamps a list of timestamps in milliseconds, sorted in chronological order.
 * Odd and even values represent beginnings and ends of periods, respectively.
 */
fun schedulerFlow(startEndTimestamps: LongArray): Flow<Boolean> {
    return flow {
        var now = System.currentTimeMillis()
        var pos = Arrays.binarySearch(startEndTimestamps, now)
        while (true) {
            val size = startEndTimestamps.size
            if (pos >= 0) {
                do {
                    pos++
                } while (pos < size && startEndTimestamps[pos] == now)
            } else {
                pos = pos.inv()
            }
            emit(pos % 2 != 0)
            if (pos == size) {
                break
            }
            // Readjust current time after suspending emit()
            delay(startEndTimestamps[pos] - System.currentTimeMillis())
            now = startEndTimestamps[pos]
        }
    }
}