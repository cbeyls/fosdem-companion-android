package be.digitalia.fosdem.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
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
    timeSource: TimeSource = TimeSource.Monotonic
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
 * @param startEndTimestamps a list of timestamps sorted in chronological order
 * @param clock the reference clock
 * Odd and even positioned values represent beginnings and ends of periods, respectively.
 */
fun schedulerFlow(
    startEndTimestamps: List<Instant>,
    clock: Clock = Clock.System
): Flow<Boolean> {
    return flow {
        var now = clock.now()
        var pos = startEndTimestamps.binarySearch(now)
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
            delay(startEndTimestamps[pos] - clock.now())
            now = startEndTimestamps[pos]
        }
    }
}