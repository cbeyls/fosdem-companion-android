package be.digitalia.fosdem.db

import androidx.room.RoomDatabase
import androidx.room.invalidationTrackerFlow
import be.digitalia.fosdem.utils.BackgroundWorkScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

fun RoomDatabase.createVersionFlow(vararg tables: String): Flow<Int> {
    return flow {
        var version = 0
        invalidationTrackerFlow(*tables).collect {
            emit(version++)
        }
    }.stateIn(
        scope = BackgroundWorkScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    ).filterNotNull()
}