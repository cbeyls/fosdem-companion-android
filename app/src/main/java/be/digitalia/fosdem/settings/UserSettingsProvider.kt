package be.digitalia.fosdem.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class UserSettingsProvider @Inject constructor(
    @ApplicationContext context: Context,
    @Named("Conference") conferenceZoneId: ZoneId
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        PreferenceManager.setDefaultValues(context, R.xml.settings, false)
    }

    private val conferenceZoneIdFlow: Flow<ZoneId> = flowOf(conferenceZoneId)
    private val deviceZoneIdFlow: StateFlow<ZoneId> by lazy(LazyThreadSafetyMode.NONE) {
        val zoneIdFlow = MutableStateFlow(ZoneId.systemDefault())
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                zoneIdFlow.value = ZoneId.systemDefault()
            }
        }, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED))
        zoneIdFlow.asStateFlow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val zoneId: Flow<ZoneId>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.USE_DEVICE_TIME_ZONE)
            .flatMapLatest { if (it) deviceZoneIdFlow else conferenceZoneIdFlow }

    val theme: Flow<Int?>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.THEME)
            .map { it?.toInt() }

    val isNotificationsEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_ENABLED)

    fun updateNotificationsEnabled(notificationsEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, notificationsEnabled)
            .apply()
    }

    val isNotificationsVibrationEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_VIBRATE)

    val isNotificationsLedEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_LED)

    val notificationsDelayInMillis: Flow<Long>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.NOTIFICATIONS_DELAY)
            .map {
                // Convert from minutes to milliseconds
                TimeUnit.MINUTES.toMillis(it?.toLong() ?: 0L)
            }
}