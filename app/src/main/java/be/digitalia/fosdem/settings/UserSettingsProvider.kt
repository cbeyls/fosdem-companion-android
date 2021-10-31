package be.digitalia.fosdem.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsProvider @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        PreferenceManager.setDefaultValues(context, R.xml.settings, false)
    }

    val theme: Flow<Int?>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.THEME)
                .map { it?.toInt() }

    val isNotificationsEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_ENABLED)

    val isNotificationsVibrationEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_VIBRATE)

    val isNotificationsLedEnabled: Flow<Boolean>
        get() = sharedPreferences.getBooleanAsFlow(PreferenceKeys.NOTIFICATIONS_LED)

    val notificationsDelayInMillis: Flow<Long>
        get() = sharedPreferences.getStringAsFlow(PreferenceKeys.NOTIFICATIONS_DELAY)
                .map {
                    // Convert from minutes to milliseconds
                    (it?.toLong() ?: 0L) * DateUtils.MINUTE_IN_MILLIS
                }
}