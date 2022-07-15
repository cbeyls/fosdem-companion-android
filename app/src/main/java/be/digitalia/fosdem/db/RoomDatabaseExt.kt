package be.digitalia.fosdem.db

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

fun RoomDatabase.createVersionFlow(vararg tables: String): StateFlow<Int> {
    val stateFlow = MutableStateFlow(0)
    invalidationTracker.addObserver(object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: Set<String>) {
            stateFlow.update { it + 1 }
        }
    })
    return stateFlow.asStateFlow()
}