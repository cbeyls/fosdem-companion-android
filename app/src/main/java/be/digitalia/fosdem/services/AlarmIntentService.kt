package be.digitalia.fosdem.services

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.StyleSpan
import androidx.annotation.RequiresApi
import androidx.core.app.*
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.set
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.activities.MainActivity
import be.digitalia.fosdem.activities.RoomImageDialogActivity
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.receivers.AlarmReceiver
import be.digitalia.fosdem.utils.PreferenceKeys
import be.digitalia.fosdem.utils.roomNameToResourceName

/**
 * A service to schedule or unschedule alarms in the background, keeping the app responsive.
 *
 * @author Christophe Beyls
 */
class AlarmIntentService : JobIntentService() {

    private lateinit var alarmManager: AlarmManager

    override fun onCreate() {
        super.onCreate()
        alarmManager = getSystemService()!!
    }

    private fun getAlarmPendingIntent(eventId: Long): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_NOTIFY_EVENT)
                .setData(eventId.toString().toUri())
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            ACTION_UPDATE_ALARMS -> {
                // Create/update all alarms
                val delay = delay
                val now = System.currentTimeMillis()
                var hasAlarms = false
                for (info in AppDatabase.getInstance(this).bookmarksDao.getBookmarksAlarmInfo(0L)) {
                    val startTime = info.startTime
                    val notificationTime = if (startTime == null) -1L else startTime.time - delay
                    val pi = getAlarmPendingIntent(info.eventId)
                    if (notificationTime < now) {
                        // Cancel pending alarms that are now scheduled in the past, if any
                        alarmManager.cancel(pi)
                    } else {
                        AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, notificationTime, pi)
                        hasAlarms = true
                    }
                }
                setAlarmReceiverEnabled(hasAlarms)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAlarms) {
                    createNotificationChannel(this)
                }
            }
            ACTION_DISABLE_ALARMS -> {
                // Cancel alarms of every bookmark in the future
                for (info in AppDatabase.getInstance(this).bookmarksDao.getBookmarksAlarmInfo(System.currentTimeMillis())) {
                    alarmManager.cancel(getAlarmPendingIntent(info.eventId))
                }
                setAlarmReceiverEnabled(false)
            }
            ACTION_ADD_BOOKMARK -> {
                val delay = delay
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)
                // Only schedule future events. If they start before the delay, the alarm will go off immediately
                if (startTime != -1L && startTime >= System.currentTimeMillis()) {
                    setAlarmReceiverEnabled(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannel(this)
                    }
                    AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, startTime - delay, getAlarmPendingIntent(eventId))
                }
            }
            ACTION_REMOVE_BOOKMARKS -> {
                // Cancel matching alarms, might they exist or not
                val eventIds = intent.getLongArrayExtra(EXTRA_EVENT_IDS)!!
                for (eventId in eventIds) {
                    alarmManager.cancel(getAlarmPendingIntent(eventId))
                }
            }
            AlarmReceiver.ACTION_NOTIFY_EVENT -> {
                val eventId = intent.dataString!!.toLong()
                val event = AppDatabase.getInstance(this).scheduleDao.getEvent(eventId)
                if (event != null) {
                    NotificationManagerCompat.from(this).notify(eventId.toInt(), buildNotification(event))
                }
            }
        }
    }

    // Convert from minutes to milliseconds
    private val delay: Long
        get() {
            val delayString = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .getString(PreferenceKeys.NOTIFICATIONS_DELAY, "0")!!
            // Convert from minutes to milliseconds
            return delayString.toLong() * DateUtils.MINUTE_IN_MILLIS
        }

    /**
     * Allows disabling the Alarm Receiver so the app is not loaded at boot when it's not necessary.
     */
    private fun setAlarmReceiverEnabled(isEnabled: Boolean) {
        val componentName = ComponentName(this, AlarmReceiver::class.java)
        val flag = if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP)
    }

    private fun buildNotification(event: Event): Notification {
        val eventPendingIntent = TaskStackBuilder
                .create(this)
                .addNextIntent(Intent(this, MainActivity::class.java))
                .addNextIntent(
                        Intent(this, EventDetailsActivity::class.java)
                                .setData(event.id.toString().toUri())
                )
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        var defaultFlags = Notification.DEFAULT_SOUND
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreferences.getBoolean(PreferenceKeys.NOTIFICATIONS_VIBRATE, false)) {
            defaultFlags = defaultFlags or Notification.DEFAULT_VIBRATE
        }

        val personsSummary = event.personsSummary
        val trackName = event.track.name
        val contentText: String
        val bigText: CharSequence?
        if (personsSummary.isNullOrEmpty()) {
            contentText = trackName
            bigText = event.subTitle
        } else {
            contentText = "$trackName - $personsSummary"
            val subTitle = event.subTitle
            val spannableBigText = if (subTitle.isNullOrEmpty()) {
                SpannableString(personsSummary)
            } else {
                SpannableString("$subTitle\n$personsSummary")
            }
            // Set the persons summary in italic
            spannableBigText[spannableBigText.length - personsSummary.length, spannableBigText.length] = StyleSpan(Typeface.ITALIC)
            bigText = spannableBigText
        }

        val notificationColor = ContextCompat.getColor(this, R.color.light_color_primary)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_fosdem)
                .setColor(notificationColor)
                .setWhen(event.startTime?.time ?: System.currentTimeMillis())
                .setContentTitle(event.title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText).setSummaryText(trackName))
                .setContentInfo(event.roomName)
                .setContentIntent(eventPendingIntent)
                .setAutoCancel(true)
                .setDefaults(defaultFlags)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Blink the LED with FOSDEM color if enabled in the options
        if (sharedPreferences.getBoolean(PreferenceKeys.NOTIFICATIONS_LED, false)) {
            notificationBuilder.setLights(notificationColor, 1000, 5000)
        }

        // Android Wear extensions
        val wearableExtender = NotificationCompat.WearableExtender()

        // Add an optional action button to show the room map image
        val roomName = event.roomName
        val roomImageResId = roomName?.let { resources.getIdentifier(roomNameToResourceName(it), "drawable", packageName) }
                ?: 0
        if (roomName != null && roomImageResId != 0) {
            // The room name is the unique Id of a RoomImageDialogActivity
            val mapIntent = Intent(this, RoomImageDialogActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(roomName.toUri())
                    .putExtra(RoomImageDialogActivity.EXTRA_ROOM_NAME, roomName)
                    .putExtra(RoomImageDialogActivity.EXTRA_ROOM_IMAGE_RESOURCE_ID, roomImageResId)
            val mapPendingIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val mapTitle = getString(R.string.room_map)
            notificationBuilder.addAction(NotificationCompat.Action(R.drawable.ic_place_white_24dp, mapTitle, mapPendingIntent))
            // Use bigger action icon for wearable notification
            wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_place_white_wear, mapTitle, mapPendingIntent))
        }

        notificationBuilder.extend(wearableExtender)
        return notificationBuilder.build()
    }

    companion object {
        /**
         * Unique job ID for this service.
         */
        private const val JOB_ID = 1000
        private const val NOTIFICATION_CHANNEL = "event_alarms"

        const val ACTION_UPDATE_ALARMS = "${BuildConfig.APPLICATION_ID}.action.UPDATE_ALARMS"
        const val ACTION_DISABLE_ALARMS = "${BuildConfig.APPLICATION_ID}.action.DISABLE_ALARMS"
        const val ACTION_ADD_BOOKMARK = "${BuildConfig.APPLICATION_ID}.action.ADD_BOOKMARK"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_START_TIME = "event_start"
        const val ACTION_REMOVE_BOOKMARKS = "${BuildConfig.APPLICATION_ID}.action.REMOVE_BOOKMARKS"
        const val EXTRA_EVENT_IDS = "event_ids"

        /**
         * Convenience method for enqueuing work in to this service.
         */
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, AlarmIntentService::class.java, JOB_ID, work)
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createNotificationChannel(context: Context) {
            val notificationManager: NotificationManager = context.getSystemService()!!
            val channel = NotificationChannel(NOTIFICATION_CHANNEL,
                    context.getString(R.string.notification_events_channel_name),
                    NotificationManager.IMPORTANCE_HIGH).apply {
                setShowBadge(false)
                lightColor = ContextCompat.getColor(context, R.color.light_color_primary)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun startChannelNotificationSettingsActivity(context: Context) {
            createNotificationChannel(context)
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }
    }
}