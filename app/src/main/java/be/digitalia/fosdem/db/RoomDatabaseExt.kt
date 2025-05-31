package be.digitalia.fosdem.db

import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

fun RoomDatabase.createVersionFlow(scope: CoroutineScope, vararg tables: String): Flow<Int> {
    return flow {
        var version = 0
        invalidationTracker.createFlow(*tables).collect {
            emit(version++)
        }
    }
        .conflate()
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1,
        )
}