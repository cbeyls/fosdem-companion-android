package be.digitalia.fosdem.alarms

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.services.AlarmIntentService
import be.digitalia.fosdem.utils.PreferenceKeys

/**
 * This class monitors bookmarks and preferences changes to dispatch alarm update work to AlarmIntentService.
 *
 * @author Christophe Beyls
 */
object FosdemAlarmManager {

    private lateinit var context: Context
    private val onSharedPreferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            PreferenceKeys.NOTIFICATIONS_ENABLED -> {
                val isEnabled = sharedPreferences.getBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, false)
                this.isEnabled = isEnabled
                if (isEnabled) {
                    startUpdateAlarms()
                } else {
                    startDisableAlarms()
                }
            }
            PreferenceKeys.NOTIFICATIONS_DELAY -> startUpdateAlarms()
        }
    }

    @MainThread
    fun init(context: Context) {
        this.context = context.applicationContext
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context)
        isEnabled = sharedPreferences.getBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, false)
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    var isEnabled: Boolean = false
        private set

    @MainThread
    fun onScheduleRefreshed() {
        if (isEnabled) {
            startUpdateAlarms()
        }
    }

    @MainThread
    fun onBookmarkAdded(event: Event) {
        if (isEnabled) {
            val serviceIntent = Intent(AlarmIntentService.ACTION_ADD_BOOKMARK).apply {
                putExtra(AlarmIntentService.EXTRA_EVENT_ID, event.id)
                event.startTime?.let {
                    putExtra(AlarmIntentService.EXTRA_EVENT_START_TIME, it.time)
                }
            }
            AlarmIntentService.enqueueWork(context, serviceIntent)
        }
    }

    @MainThread
    fun onBookmarksRemoved(eventIds: LongArray?) {
        if (isEnabled) {
            val serviceIntent = Intent(AlarmIntentService.ACTION_REMOVE_BOOKMARKS)
                    .putExtra(AlarmIntentService.EXTRA_EVENT_IDS, eventIds)
            AlarmIntentService.enqueueWork(context, serviceIntent)
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