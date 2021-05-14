package be.digitalia.fosdem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.services.AlarmIntentService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry point for system-generated events: boot complete and alarms.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmManager: FosdemAlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NOTIFY_EVENT -> {
                val serviceIntent = Intent(ACTION_NOTIFY_EVENT)
                    .setData(intent.data)
                AlarmIntentService.enqueueWork(context, serviceIntent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val serviceAction = if (alarmManager.isEnabled) AlarmIntentService.ACTION_UPDATE_ALARMS
                else AlarmIntentService.ACTION_DISABLE_ALARMS
                val serviceIntent = Intent(serviceAction)
                AlarmIntentService.enqueueWork(context, serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_NOTIFY_EVENT = BuildConfig.APPLICATION_ID + ".action.NOTIFY_EVENT"
    }
}