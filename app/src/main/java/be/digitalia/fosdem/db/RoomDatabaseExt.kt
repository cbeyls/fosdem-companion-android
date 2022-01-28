package be.digitalia.fosdem.db

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import be.digitalia.fosdem.flow.flowWhileShared
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update

fun RoomDatabase.createVersionFlow(vararg tables: String): StateFlow<Int> {
    val stateFlow = MutableStateFlow(0)
    invalidationTracker.addObserver(object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: MutableSet<String>) {
            stateFlow.update { it + 1 }
        }
    })
    return stateFlow.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> observableQuery(
    version: StateFlow<Int>,
    subscriptionCount: StateFlow<Int>,
    producer: suspend (version: Int) -> T
): Flow<T> {
    return version
        .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed())
        .distinctUntilChanged()
        .mapLatest(producer)
}