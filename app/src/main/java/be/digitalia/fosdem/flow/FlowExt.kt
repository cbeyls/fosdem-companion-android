package be.digitalia.fosdem.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

fun <T> sharedFlow(
    scope: CoroutineScope,
    replay: Int = 1,
    producer: (subscriptionCount: StateFlow<Int>) -> Flow<T>
): SharedFlow<T> {
    val shared = MutableSharedFlow<T>(replay = replay)
    scope.launch {
        producer(shared.subscriptionCount).collect(shared)
    }
    return shared.asSharedFlow()
}

fun <T> stateFlow(
    scope: CoroutineScope,
    initialValue: T,
    producer: (subscriptionCount: StateFlow<Int>) -> Flow<T>
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