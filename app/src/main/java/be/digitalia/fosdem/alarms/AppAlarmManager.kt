package be.digitalia.fosdem.alarms

import android.content.Context
import android.content.Intent
import be.digitalia.fosdem.model.AlarmInfo
import be.digitalia.fosdem.services.AlarmIntentService
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class monitors bookmarks and settings changes to dispatch alarm update work to AlarmIntentService.
 */
@Singleton
class AppAlarmManager @Inject constructor(
        @ApplicationContext private val context: Context,
        private val userSettingsProvider: UserSettingsProvider
) {

    init {
        BackgroundWorkScope.launch {
            // Skip initial value and only act on changes
            userSettingsProvider.isNotificationsEnabled.drop(1).collect { isEnabled ->
                onEnabledChanged(isEnabled)
            }
        }
        BackgroundWorkScope.launch {
            userSettingsProvider.notificationsDelayInMillis.drop(1).collect {
                if (userSettingsProvider.isNotificationsEnabled.first()) {
                    startUpdateAlarms()
                }
            }
        }
    }

    suspend fun onBootCompleted() {
        onEnabledChanged(userSettingsProvider.isNotificationsEnabled.first())
    }

    suspend fun onScheduleRefreshed() {
        if (userSettingsProvider.isNotificationsEnabled.first()) {
            startUpdateAlarms()
        }
    }

    suspend fun onBookmarksAdded(alarmInfos: List<AlarmInfo>) {
        if (userSettingsProvider.isNotificationsEnabled.first()) {
            val arrayList = if (alarmInfos is ArrayList<AlarmInfo>) alarmInfos else ArrayList(alarmInfos)
            val serviceIntent = Intent(AlarmIntentService.ACTION_ADD_BOOKMARKS)
                    .putParcelableArrayListExtra(AlarmIntentService.EXTRA_ALARM_INFOS, arrayList)
            AlarmIntentService.enqueueWork(context, serviceIntent)
        }
    }

    suspend fun onBookmarksRemoved(eventIds: LongArray) {
        if (userSettingsProvider.isNotificationsEnabled.first()) {
            val serviceIntent = Intent(AlarmIntentService.ACTION_REMOVE_BOOKMARKS)
                    .putExtra(AlarmIntentService.EXTRA_EVENT_IDS, eventIds)
            AlarmIntentService.enqueueWork(context, serviceIntent)
        }
    }

    private fun onEnabledChanged(isEnabled: Boolean) {
        if (isEnabled) {
            startUpdateAlarms()
        } else {
            startDisableAlarms()
        }
    }

    private fun startUpdateAlarms() {
        val serviceIntent = Intent(AlarmIntentService.ACTION_UPDATE_ALARMS)
        AlarmIntentService.enqueueWork(context, serviceIntent)
    }

    private fun startDisableAlarms() {
        val serviceIntent = Intent(AlarmIntentService.ACTION_DISABLE_ALARMS)
        AlarmIntentService.enqueueWork(context, serviceIntent)
    }
}