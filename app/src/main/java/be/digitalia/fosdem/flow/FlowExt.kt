package be.digitalia.fosdem.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

inline fun <T> stateFlow(
    scope: CoroutineScope,
    initialValue: T,
    producer: (subscriptionCount: StateFlow<Int>) -> Flow<T>
): StateFlow<T> {
    val state = MutableStateFlow(initialValue)
    producer(state.subscriptionCount).launchIn(scope, state)
    return state.asStateFlow()
}

fun <T> Flow<T>.launchIn(scope: CoroutineScope, collector: FlowCollector<T>): Job = scope.launch {
    collect(collector)
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

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> versionedResourceFlow(
    version: StateFlow<Int>,
    subscriptionCount: StateFlow<Int>,
    producer: suspend (version: Int) -> T
): Flow<T> {
    return version
        .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed())
        .distinctUntilChanged()
        .mapLatest(producer)
}