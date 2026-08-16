package be.digitalia.fosdem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.inject.appGraph
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.goAsync

/**
 * Entry point for system-generated events: boot complete and alarms.
 *
 * @author Christophe Beyls
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmManager = context.appGraph.alarmManager
        when (intent.action) {
            ACTION_NOTIFY_EVENT -> intent.dataString?.toLongOrNull()?.let { eventId ->
                goAsync(BackgroundWorkScope) {
                    alarmManager.notifyEvent(eventId)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> goAsync(BackgroundWorkScope) {
                alarmManager.onBootCompleted()
            }
        }
    }

    companion object {
        const val ACTION_NOTIFY_EVENT = BuildConfig.APPLICATION_ID + ".action.NOTIFY_EVENT"
    }
}
