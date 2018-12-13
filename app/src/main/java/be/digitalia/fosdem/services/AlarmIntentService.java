package be.digitalia.fosdem.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.AlarmManagerCompat;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.activities.MainActivity;
import be.digitalia.fosdem.activities.RoomImageDialogActivity;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.SettingsFragment;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.receivers.AlarmReceiver;
import be.digitalia.fosdem.utils.StringUtils;

/**
 * A service to schedule or unschedule alarms in the background, keeping the app responsive.
 *
 * @author Christophe Beyls
 */
public class AlarmIntentService extends JobIntentService {

	/**
	 * Unique job ID for this service.
	 */
	private static final int JOB_ID = 1000;
	private static final String NOTIFICATION_CHANNEL = "event_alarms";

	public static final String ACTION_UPDATE_ALARMS = BuildConfig.APPLICATION_ID + ".action.UPDATE_ALARMS";
	public static final String ACTION_DISABLE_ALARMS = BuildConfig.APPLICATION_ID + ".action.DISABLE_ALARMS";

	private AlarmManager alarmManager;

	/**
	 * Convenience method for enqueuing work in to this service.
	 */
	public static void enqueueWork(Context context, Intent work) {
		enqueueWork(context, AlarmIntentService.class, JOB_ID, work);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	}

	private PendingIntent getAlarmPendingIntent(long eventId) {
		Intent intent = new Intent(this, AlarmReceiver.class)
				.setAction(AlarmReceiver.ACTION_NOTIFY_EVENT)
				.setData(Uri.parse(String.valueOf(eventId)));
		return PendingIntent.getBroadcast(this, 0, intent, 0);
	}

	@Override
	protected void onHandleWork(@NonNull Intent intent) {
		switch (intent.getAction()) {

			case ACTION_UPDATE_ALARMS: {

				// Create/update all alarms
				final long delay = getDelay();
				final long now = System.currentTimeMillis();
				boolean hasAlarms = false;
				Cursor cursor = DatabaseManager.getInstance().getBookmarks(0L);
				try {
					while (cursor.moveToNext()) {
						long eventId = DatabaseManager.toEventId(cursor);
						long notificationTime = DatabaseManager.toEventStartTimeMillis(cursor) - delay;
						PendingIntent pi = getAlarmPendingIntent(eventId);
						if (notificationTime < now) {
							// Cancel pending alarms that are now scheduled in the past, if any
							alarmManager.cancel(pi);
						} else {
							AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, notificationTime, pi);
							hasAlarms = true;
						}
					}

				} finally {
					cursor.close();
				}
				setAlarmReceiverEnabled(hasAlarms);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAlarms) {
					createNotificationChannel(this);
				}

				break;
			}
			case ACTION_DISABLE_ALARMS: {

				// Cancel alarms of every bookmark in the future
				Cursor cursor = DatabaseManager.getInstance().getBookmarks(System.currentTimeMillis());
				try {
					while (cursor.moveToNext()) {
						long eventId = DatabaseManager.toEventId(cursor);
						alarmManager.cancel(getAlarmPendingIntent(eventId));
					}
				} finally {
					cursor.close();
				}
				setAlarmReceiverEnabled(false);

				break;
			}
			case DatabaseManager.ACTION_ADD_BOOKMARK: {

				long delay = getDelay();
				long eventId = intent.getLongExtra(DatabaseManager.EXTRA_EVENT_ID, -1L);
				long startTime = intent.getLongExtra(DatabaseManager.EXTRA_EVENT_START_TIME, -1L);
				// Only schedule future events. If they start before the delay, the alarm will go off immediately
				if ((startTime == -1L) || (startTime < System.currentTimeMillis())) {
					break;
				}
				setAlarmReceiverEnabled(true);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					createNotificationChannel(this);
				}
				AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, startTime - delay, getAlarmPendingIntent(eventId));

				break;
			}
			case DatabaseManager.ACTION_REMOVE_BOOKMARKS: {

				// Cancel matching alarms, might they exist or not
				long[] eventIds = intent.getLongArrayExtra(DatabaseManager.EXTRA_EVENT_IDS);
				for (long eventId : eventIds) {
					alarmManager.cancel(getAlarmPendingIntent(eventId));
				}

				break;
			}
			case AlarmReceiver.ACTION_NOTIFY_EVENT: {

				long eventId = Long.parseLong(intent.getDataString());
				Event event = DatabaseManager.getInstance().getEvent(eventId);
				if (event != null) {
					NotificationManagerCompat.from(this).notify((int) eventId, buildNotification(event));
				}

				break;
			}
		}
	}

	private long getDelay() {
		String delayString = PreferenceManager.getDefaultSharedPreferences(this).getString(
				SettingsFragment.KEY_PREF_NOTIFICATIONS_DELAY, "0");
		// Convert from minutes to milliseconds
		return Long.parseLong(delayString) * DateUtils.MINUTE_IN_MILLIS;
	}

	/**
	 * Allows disabling the Alarm Receiver so the app is not loaded at boot when it's not necessary.
	 */
	private void setAlarmReceiverEnabled(boolean isEnabled) {
		ComponentName componentName = new ComponentName(this, AlarmReceiver.class);
		int flag = isEnabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		getPackageManager().setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private static void createNotificationChannel(Context context) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
				context.getString(R.string.notification_events_channel_name),
				NotificationManager.IMPORTANCE_HIGH);
		channel.setShowBadge(false);
		channel.setLightColor(context.getColor(R.color.color_primary));
		channel.enableVibration(true);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
		notificationManager.createNotificationChannel(channel);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public static void startChannelNotificationSettingsActivity(Context context) {
		createNotificationChannel(context);

		Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
				.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL)
				.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
		context.startActivity(intent);
	}

	private Notification buildNotification(Event event) {
		PendingIntent eventPendingIntent = TaskStackBuilder
				.create(this)
				.addNextIntent(new Intent(this, MainActivity.class))
				.addNextIntent(
						new Intent(this, EventDetailsActivity.class)
								.setData(Uri.parse(String.valueOf(event.getId())))
				)
				.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		int defaultFlags = Notification.DEFAULT_SOUND;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_VIBRATE, false)) {
			defaultFlags |= Notification.DEFAULT_VIBRATE;
		}

		String personsSummary = event.getPersonsSummary();
		String trackName = event.getTrack().getName();
		String contentText;
		CharSequence bigText;
		if (TextUtils.isEmpty(personsSummary)) {
			contentText = trackName;
			bigText = event.getSubTitle();
		} else {
			contentText = String.format("%1$s - %2$s", trackName, personsSummary);
			String subTitle = event.getSubTitle();
			SpannableString spannableBigText;
			if (TextUtils.isEmpty(subTitle)) {
				spannableBigText = new SpannableString(personsSummary);
			} else {
				spannableBigText = new SpannableString(String.format("%1$s\n%2$s", subTitle, personsSummary));
			}
			// Set the persons summary in italic
			spannableBigText.setSpan(new StyleSpan(Typeface.ITALIC),
					spannableBigText.length() - personsSummary.length(), spannableBigText.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			bigText = spannableBigText;
		}

		int notificationColor = ContextCompat.getColor(this, R.color.color_primary);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
				.setSmallIcon(R.drawable.ic_stat_fosdem)
				.setColor(notificationColor)
				.setWhen(event.getStartTime().getTime())
				.setContentTitle(event.getTitle())
				.setContentText(contentText)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText).setSummaryText(trackName))
				.setContentInfo(event.getRoomName())
				.setContentIntent(eventPendingIntent)
				.setAutoCancel(true)
				.setDefaults(defaultFlags)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_EVENT)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		// Blink the LED with FOSDEM color if enabled in the options
		if (sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_LED, false)) {
			notificationBuilder.setLights(notificationColor, 1000, 5000);
		}

		// Android Wear extensions
		NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

		// Add an optional action button to show the room map image
		String roomName = event.getRoomName();
		int roomImageResId = getResources().getIdentifier(StringUtils.roomNameToResourceName(roomName),
				"drawable", getPackageName());
		if (roomImageResId != 0) {
			// The room name is the unique Id of a RoomImageDialogActivity
			Intent mapIntent = new Intent(this, RoomImageDialogActivity.class).setFlags(
					Intent.FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse(roomName));
			mapIntent.putExtra(RoomImageDialogActivity.EXTRA_ROOM_NAME, roomName);
			mapIntent.putExtra(RoomImageDialogActivity.EXTRA_ROOM_IMAGE_RESOURCE_ID, roomImageResId);
			PendingIntent mapPendingIntent = PendingIntent.getActivity(this, 0, mapIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			CharSequence mapTitle = getString(R.string.room_map);
			notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_place_white_24dp, mapTitle,
					mapPendingIntent));
			// Use bigger action icon for wearable notification
			wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_place_white_wear, mapTitle,
					mapPendingIntent));
		}

		notificationBuilder.extend(wearableExtender);
		return notificationBuilder.build();
	}
}
