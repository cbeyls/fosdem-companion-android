package be.digitalia.fosdem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.services.AlarmIntentService
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for system-generated events: boot complete and alarms.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmManager: AppAlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NOTIFY_EVENT -> {
                val serviceIntent = Intent(ACTION_NOTIFY_EVENT)
                    .setData(intent.data)
                AlarmIntentService.enqueueWork(context, serviceIntent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                BackgroundWorkScope.launch {
                    alarmManager.onBootCompleted()
                }
            }
        }
    }

    companion object {
        const val ACTION_NOTIFY_EVENT = BuildConfig.APPLICATION_ID + ".action.NOTIFY_EVENT"
    }
}