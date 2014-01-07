package be.digitalia.fosdem.services;

import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.activities.MainActivity;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.SettingsFragment;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.receivers.AlarmReceiver;

/**
 * A service to schedule or unschedule alarms in the background, keeping the app responsive.
 * 
 * @author Christophe Beyls
 * 
 */
public class AlarmIntentService extends IntentService {

	public static final String ACTION_UPDATE_ALARMS = "be.digitalia.fosdem.action.UPDATE_ALARMS";
	public static final String EXTRA_WITH_WAKE_LOCK = "with_wake_lock";
	public static final String ACTION_DISABLE_ALARMS = "be.digitalia.fosdem.action.DISABLE_ALARMS";

	private AlarmManager alarmManager;

	public AlarmIntentService() {
		super("AlarmIntentService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Ask for the last unhandled intents to be redelivered if the service dies early.
		// This ensures we handle all events, in order.
		setIntentRedelivery(true);

		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	}

	private PendingIntent getAlarmPendingIntent(int eventId) {
		Intent intent = new Intent(this, AlarmReceiver.class).setAction(AlarmReceiver.ACTION_NOTIFY_EVENT).setData(Uri.parse(String.valueOf(eventId)));
		return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();

		if (ACTION_UPDATE_ALARMS.equals(action)) {

			// Create/update all alarms
			long delay = getDelay();
			long now = System.currentTimeMillis();
			Cursor cursor = DatabaseManager.getInstance().getBookmarks(now);
			try {
				while (cursor.moveToNext()) {
					int eventId = DatabaseManager.toEventId(cursor);
					long notificationTime = DatabaseManager.toEventStartTimeMillis(cursor) - delay;
					PendingIntent pi = getAlarmPendingIntent(eventId);
					if (notificationTime < now) {
						// Cancel pending alarms that where scheduled between now and delay, if any
						alarmManager.cancel(pi);
					} else {
						alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pi);
					}
				}
			} finally {
				cursor.close();
			}

			// Release the wake lock setup by AlarmReceiver, if any
			if (intent.getBooleanExtra(EXTRA_WITH_WAKE_LOCK, false)) {
				AlarmReceiver.completeWakefulIntent(intent);
			}

		} else if (ACTION_DISABLE_ALARMS.equals(action)) {

			// Cancel alarms of every bookmark in the future
			Cursor cursor = DatabaseManager.getInstance().getBookmarks(System.currentTimeMillis());
			try {
				while (cursor.moveToNext()) {
					int eventId = DatabaseManager.toEventId(cursor);
					alarmManager.cancel(getAlarmPendingIntent(eventId));
				}
			} finally {
				cursor.close();
			}

		} else if (DatabaseManager.ACTION_ADD_BOOKMARK.equals(action)) {

			long delay = getDelay();
			Event event = intent.getParcelableExtra(DatabaseManager.EXTRA_EVENT);
			// Only schedule future events. If they start before the delay, the alarm will go off immediately
			Date startTime = event.getStartTime();
			if ((startTime == null) || (startTime.getTime() < System.currentTimeMillis())) {
				return;
			}
			alarmManager.set(AlarmManager.RTC_WAKEUP, startTime.getTime() - delay, getAlarmPendingIntent(event.getId()));

		} else if (DatabaseManager.ACTION_REMOVE_BOOKMARKS.equals(action)) {

			// Cancel matching alarms, might they exist or not
			int[] eventIds = intent.getIntArrayExtra(DatabaseManager.EXTRA_EVENT_IDS);
			for (int eventId : eventIds) {
				alarmManager.cancel(getAlarmPendingIntent(eventId));
			}
		} else if (AlarmReceiver.ACTION_NOTIFY_EVENT.equals(action)) {

			int eventId = Integer.parseInt(intent.getDataString());
			Event event = DatabaseManager.getInstance().getEvent(eventId);
			if (event != null) {

				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

				PendingIntent eventPendingIntent = TaskStackBuilder.create(this).addNextIntent(new Intent(this, MainActivity.class))
						.addNextIntent(new Intent(this, EventDetailsActivity.class).setData(Uri.parse(String.valueOf(event.getId()))))
						.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

				int defaultFlags = Notification.DEFAULT_SOUND;
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				if (sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_VIBRATE, false)) {
					defaultFlags |= Notification.DEFAULT_VIBRATE;
				}
				if (sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_LED, false)) {
					defaultFlags |= Notification.DEFAULT_LIGHTS;
				}

				Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher).setWhen(event.getStartTime().getTime())
						.setContentTitle(event.getTitle()).setContentText(String.format("%1$s - %2$s", event.getRoomName(), event.getPersonsSummary()))
						.setContentInfo(event.getTrack().getName()).setContentIntent(eventPendingIntent).setAutoCancel(true).setDefaults(defaultFlags)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
				notificationManager.notify(eventId, notification);
			}

			AlarmReceiver.completeWakefulIntent(intent);
		}
	}

	private long getDelay() {
		String delayString = PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsFragment.KEY_PREF_NOTIFICATIONS_DELAY, "0");
		// Convert from minutes to milliseconds
		return Long.parseLong(delayString) * 1000L * 60L;
	}
}
