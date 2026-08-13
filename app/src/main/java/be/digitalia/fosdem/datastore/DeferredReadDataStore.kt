package be.digitalia.fosdem.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Prevents reading the wrapped DataStore until the provided Job completes.
 */
class DeferredReadDataStore<T>(wrapped: DataStore<T>, deferred: Job) : DataStore<T> by wrapped {
    override val data: Flow<T> = wrapped.data.onStart { deferred.join() }
}
