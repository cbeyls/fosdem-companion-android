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

@JvmInline
value class SharedFlowContext(private val subscriptionCount: StateFlow<Int>) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> Flow<T>.flowWhileShared(started: SharingStarted): Flow<T> {
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
}

inline fun <T> stateFlow(
    scope: CoroutineScope,
    initialValue: T,
    producer: SharedFlowContext.() -> Flow<T>
): StateFlow<T> {
    val state = MutableStateFlow(initialValue)
    producer(SharedFlowContext(state.subscriptionCount)).launchIn(scope, state)
    return state.asStateFlow()
}

fun <T> Flow<T>.launchIn(scope: CoroutineScope, collector: FlowCollector<T>): Job = scope.launch {
    collect(collector)
}

inline fun <T> countSubscriptionsFlow(producer: SharedFlowContext.() -> Flow<T>): Flow<T> {
    val subscriptionCount = MutableStateFlow(0)
    return producer(SharedFlowContext(subscriptionCount.asStateFlow()))
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
fun <T> SharedFlowContext.versionedResourceFlow(
    version: Flow<Int>,
    producer: suspend (version: Int) -> T
): Flow<T> {
    return version
        .flowWhileShared(SharingStarted.WhileSubscribed())
        .distinctUntilChanged()
        .mapLatest(producer)
}