package be.digitalia.fosdem.settings

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart

/**
 * Extensions to read or observe SharedPreferences values as a Kotlin Flow
 */

fun SharedPreferences.getStringAsFlow(key: String): Flow<String?> {
    return getAsFlow(key) { providerKey ->
        getString(providerKey, null)
    }
}

fun SharedPreferences.getBooleanAsFlow(key: String): Flow<Boolean> {
    return getAsFlow(key) { providerKey ->
        getBoolean(providerKey, false)
    }
}

private inline fun <T> SharedPreferences.getAsFlow(
        key: String,
        crossinline valueProvider: SharedPreferences.(key: String) -> T
): Flow<T> {
    return callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, listenerKey ->
            if (listenerKey == key) {
                trySend(sharedPreferences.valueProvider(listenerKey))
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().onStart {
        // By emitting the initial value here, we prevent the upstream callbackFlow from starting
        // if the Flow consumer is only interested in the first value
        emit(valueProvider(key))
    }
}