package be.digitalia.fosdem.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Inject
@SingleIn(AppScope::class)
class UserSettingsProvider(
    private val sharedPreferences: SharedPreferences
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val timeZoneMode: Flow<TimeZoneMode> =
        sharedPreferences.getBooleanAsFlow(PreferenceKeys.USE_DEVICE_TIME_ZONE)
            .map { if (it) TimeZoneMode.DEVICE else TimeZoneMode.DEFAULT }

    val theme: Flow<Int?> = sharedPreferences.getStringAsFlow(PreferenceKeys.THEME)
        .map { it?.toInt() }

    val isNotificationsEnabled: Flow<Boolean> =
        sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_ENABLED)

    fun updateNotificationsEnabled(notificationsEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, notificationsEnabled)
        }
    }

    val isNotificationsVibrationEnabled: Flow<Boolean> =
        sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_VIBRATE)

    val isNotificationsLedEnabled: Flow<Boolean> =
        sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_LED)

    val notificationsDelay: Flow<Duration> =
        sharedPreferences.getStringAsFlow(PreferenceKeys.NOTIFICATIONS_DELAY)
            .map { stringDelayInMinutes ->
                stringDelayInMinutes?.toLong()?.minutes ?: Duration.ZERO
            }
}
