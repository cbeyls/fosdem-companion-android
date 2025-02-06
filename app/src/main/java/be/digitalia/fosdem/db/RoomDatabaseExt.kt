package be.digitalia.fosdem.db

import androidx.room.RoomDatabase
import androidx.room.invalidationTrackerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun RoomDatabase.createVersionFlow(scope: CoroutineScope, vararg tables: String): StateFlow<Int> {
    val versionFlow = MutableStateFlow(0)
    scope.launch {
        invalidationTrackerFlow(tables = tables, emitInitialState = false).collect {
            versionFlow.update { it + 1 }
        }
    }
    return versionFlow.asStateFlow()
}