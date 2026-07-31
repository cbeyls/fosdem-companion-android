package be.digitalia.fosdem.flow

import androidx.datastore.core.DataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Prevents reading the wrapped DataStore until the provided Job completes.
 */
class DeferredReadDataStore<T>(
    private val wrapped: DataStore<T>,
    private val deferred: Job
) : DataStore<T> by wrapped {
    override val data: Flow<T>
        get() = wrapped.data.onStart { deferred.join() }
}