package be.digitalia.fosdem.flow

import androidx.datastore.core.DataStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Prevents reading the wrapped DataStore until the provided Deferred completes.
 */
class DeferredReadDataStore<T>(
    private val wrapped: DataStore<T>,
    private val deferred: Deferred<Any>
) : DataStore<T> {
    override val data: Flow<T>
        get() = wrapped.data.onStart { deferred.await() }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        return wrapped.updateData(transform)
    }
}