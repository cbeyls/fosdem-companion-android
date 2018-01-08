package be.digitalia.fosdem.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.alarms.FosdemAlarmManager;
import be.digitalia.fosdem.services.AlarmIntentService;

/**
 * Entry point for system-generated events: boot complete and alarms.
 *
 * @author Christophe Beyls
 */
public class AlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_NOTIFY_EVENT = BuildConfig.APPLICATION_ID + ".action.NOTIFY_EVENT";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (ACTION_NOTIFY_EVENT.equals(action)) {

			// Forward the intent to the AlarmIntentService for background processing of the notification
			Intent serviceIntent = new Intent(ACTION_NOTIFY_EVENT)
					.setData(intent.getData());
			AlarmIntentService.enqueueWork(context, serviceIntent);

		} else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

			String serviceAction = FosdemAlarmManager.getInstance().isEnabled()
					? AlarmIntentService.ACTION_UPDATE_ALARMS : AlarmIntentService.ACTION_DISABLE_ALARMS;
			Intent serviceIntent = new Intent(serviceAction);
			AlarmIntentService.enqueueWork(context, serviceIntent);
		}
	}

}
