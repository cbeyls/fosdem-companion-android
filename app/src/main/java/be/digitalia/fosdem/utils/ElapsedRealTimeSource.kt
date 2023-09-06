package be.digitalia.fosdem.utils

import android.os.SystemClock
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit

object ElapsedRealTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
    override fun read(): Long = SystemClock.elapsedRealtimeNanos()
    override fun toString(): String = "TimeSource(SystemClock.elapsedRealtimeNanos())"
}