package be.digitalia.fosdem.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A DataStore decorator which uses an in-memory copy as single source of truth and defers all disk writes
 * to a secondary coroutine launched in the provided scope.
 * I/O write errors are ignored. A new write operation will be attempted every time the value changes.
 * Calls to updateData() will complete and data will emit the new value as soon as the transform operation completes.
 *
 * Note that if no transform operation in updateData() suspends, the last value produced by a call to updateData()
 * started before collecting data is guaranteed to be the first one emitted if both calls happen on the main thread.
 * This is ideal for storing UI states where updates need to be visible immediately.
 */
class DeferredWriteDataStore<T>(wrapped: DataStore<T>, scope: CoroutineScope) : DataStore<T> {
    private val deferredStateFlow: Deferred<MutableStateFlow<T>> = scope.async {
        MutableStateFlow(wrapped.data.first()).also {
            scope.launch {
                it.collectLatest { value ->
                    try {
                        wrapped.updateData { value }
                    } catch (_: IOException) {
                    }
                }
            }
        }
    }

    // Use a mutex to preserve the guarantee that updateData() operations are serialized.
    private val updateMutex = Mutex()

    override val data: Flow<T> = flow {
        deferredStateFlow.await().collect(this)
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        return updateMutex.withLock {
            deferredStateFlow.await().let {
                // No need to perform an atomic CAS because we are already using a mutex lock
                val prevValue = it.value
                val nextValue = transform(prevValue)
                it.value = nextValue
                nextValue
            }
        }
    }
}
