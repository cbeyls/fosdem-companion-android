package be.digitalia.fosdem.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import be.digitalia.fosdem.utils.AppTimeSource
import java.util.Arrays
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

fun tickerFlow(period: Duration): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(period)
    }
}

/**
 * Creates a ticker Flow which delays emitting a value until there is at least one subscription.
 * timeSource needs to be monotonic.
 */
fun SharedFlowContext.synchronizedTickerFlow(
    period: Duration,
    timeSource: TimeSource
): Flow<Unit> {
    return flow {
        var nextEmissionTimeMark: TimeMark? = null
        flow {
            nextEmissionTimeMark?.let { delay(-it.elapsedNow()) }
            while (true) {
                emit(Unit)
                nextEmissionTimeMark = timeSource.markNow() + period
                delay(period)
            }
        }
            .flowWhileShared(SharingStarted.WhileSubscribed())
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
        var now = AppTimeSource.currentTimeMillis()
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
            delay(startEndTimestamps[pos] - AppTimeSource.currentTimeMillis())
            now = startEndTimestamps[pos]
        }
    }
}