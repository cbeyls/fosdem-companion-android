package be.digitalia.fosdem.flow

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.util.Arrays

fun tickerFlow(periodInMillis: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodInMillis)
    }
}

/**
 * Creates a ticker Flow which only emits when subscriptionCount > 0.
 */
fun whileSubscribedTickerFlow(subscriptionCount: StateFlow<Int>, periodInMillis: Long): Flow<Unit> {
    return flow {
        var nextEmissionTime = 0L
        flow {
            delay(nextEmissionTime - SystemClock.elapsedRealtime())
            while (true) {
                emit(Unit)
                nextEmissionTime = SystemClock.elapsedRealtime() + periodInMillis
                delay(periodInMillis)
            }
        }
            .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed())
            .collect(this)
    }
}

/**
 * Builds a Flow whose value is true during scheduled periods.
 *
 * @param startEndTimestamps a list of timestamps in milliseconds, sorted in chronological order.
 * Odd and even values represent beginnings and ends of periods, respectively.
 */
fun schedulerFlow(vararg startEndTimestamps: Long): Flow<Boolean> {
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