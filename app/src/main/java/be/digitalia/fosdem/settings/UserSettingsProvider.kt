package be.digitalia.fosdem.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class UserSettingsProvider @Inject constructor(
    @ApplicationContext context: Context,
    @Named("UserSettings") private val sharedPreferences: SharedPreferences
) {
    private val deviceZoneIdFlow: StateFlow<ZoneId> by lazy(LazyThreadSafetyMode.NONE) {
        val zoneIdFlow = MutableStateFlow(ZoneId.systemDefault())
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                zoneIdFlow.value = ZoneId.systemDefault()
            }
        }, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
        zoneIdFlow.asStateFlow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val timeZoneMode: Flow<TimeZoneMode>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.USE_DEVICE_TIME_ZONE)
            .flatMapLatest {
                if (it) deviceZoneIdFlow.map { zoneId -> TimeZoneMode.Device(zoneId) }
                else flowOf(TimeZoneMode.Default)
            }

    val theme: Flow<Int?>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.THEME)
            .map { it?.toInt() }

    val isNotificationsEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_ENABLED)

    fun updateNotificationsEnabled(notificationsEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, notificationsEnabled)
        }
    }

    val isNotificationsVibrationEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_VIBRATE)

    val isNotificationsLedEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_LED)

    val notificationsDelay: Flow<Duration>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.NOTIFICATIONS_DELAY)
            .map { stringDelayInMinutes ->
                stringDelayInMinutes?.let { Duration.ofMinutes(it.toLong()) } ?: Duration.ZERO
            }
}