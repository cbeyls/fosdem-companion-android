package be.digitalia.fosdem.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant

object DebugClock {
    private val _offsetFlow = MutableStateFlow<Duration?>(null)

    /** Observable offset. Collectors restart their time logic on each change. */
    val offsetFlow: StateFlow<Duration?> = _offsetFlow

    var offset: Duration?
        get() = _offsetFlow.value
        set(value) { _offsetFlow.value = value }

    fun now(): Instant = offset?.let { Instant.now().plus(it) } ?: Instant.now()

    fun currentTimeMillis(): Long = offset?.let {
        System.currentTimeMillis() + it.toMillis()
    } ?: System.currentTimeMillis()
}
