package be.digitalia.fosdem.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

inline fun <T> stateFlow(
    scope: CoroutineScope,
    initialValue: T,
    crossinline producer: (subscriptionCount: StateFlow<Int>) -> Flow<T>
): StateFlow<T> {
    val state = MutableStateFlow(initialValue)
    scope.launch {
        producer(state.subscriptionCount).collect(state)
    }
    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.flowWhileShared(
    subscriptionCount: StateFlow<Int>,
    started: SharingStarted
): Flow<T> {
    return started.command(subscriptionCount)
        .distinctUntilChanged()
        .flatMapLatest {
            when (it) {
                SharingCommand.START -> this
                SharingCommand.STOP,
                SharingCommand.STOP_AND_RESET_REPLAY_CACHE -> emptyFlow()
            }
        }
}

inline fun <T> countSubscriptionsFlow(producer: (subscriptionCount: StateFlow<Int>) -> Flow<T>): Flow<T> {
    val subscriptionCount = MutableStateFlow(0)
    return producer(subscriptionCount.asStateFlow())
        .countSubscriptionsTo(subscriptionCount)
}

fun <T> Flow<T>.countSubscriptionsTo(subscriptionCount: MutableStateFlow<Int>): Flow<T> {
    return flow {
        subscriptionCount.update { it + 1 }
        try {
            collect(this)
        } finally {
            subscriptionCount.update { it - 1 }
        }
    }
}