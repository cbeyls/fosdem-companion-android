package be.digitalia.fosdem.alarms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.activities.MainActivity
import be.digitalia.fosdem.activities.RoomImageDialogActivity
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.AlarmInfo
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.receivers.AlarmReceiver
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.roomNameToResourceName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class monitors incoming broadcasts and bookmarks and settings changes to dispatch background alarm update work.
 */
@Singleton
class AppAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsProvider: UserSettingsProvider,
    private val bookmarksDao: BookmarksDao,
    private val scheduleDao: ScheduleDao
) {
    private val alarmManager: AlarmManager by lazy(LazyThreadSafetyMode.NONE) {
        requireNotNull(context.getSystemService())
    }
    private val queueMutex = Mutex()

    private suspend fun isNotificationsEnabled(): Boolean =
        userSettingsProvider.isNotificationsEnabled.first()

    init {
        // Skip initial values and only act on changes
        BackgroundWorkScope.launch {
            userSettingsProvider.isNotificationsEnabled.drop(1).collect { isEnabled ->
                onEnabledChanged(isEnabled)
            }
        }
        BackgroundWorkScope.launch {
            userSettingsProvider.notificationsDelayInMillis.drop(1).collect {
                if (isNotificationsEnabled()) {
                    updateAlarms()
                }
            }
        }
    }

    suspend fun onBootCompleted() {
        onEnabledChanged(isNotificationsEnabled())
    }

    suspend fun onScheduleRefreshed() {
        if (isNotificationsEnabled()) {
            updateAlarms()
        }
    }

    suspend fun onBookmarksAdded(alarmInfos: List<AlarmInfo>) {
        if (alarmInfos.isEmpty() || !isNotificationsEnabled()) return

        queueMutex.withLock {
            val delay = userSettingsProvider.notificationsDelayInMillis.first()
            val now = System.currentTimeMillis()
            var isFirstAlarm = true
            for ((eventId, startTime) in alarmInfos) {
                // Only schedule future events. If they start before the delay, the alarm will go off immediately
                if (startTime != null && startTime.toEpochMilli() >= now) {
                    if (isFirstAlarm) {
                        setAlarmReceiverEnabled(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            createNotificationChannel(context)
                        }
                        isFirstAlarm = false
                    }
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager, AlarmManager.RTC_WAKEUP,
                        startTime.toEpochMilli() - delay, getAlarmPendingIntent(eventId)
                    )
                }
            }
        }
    }

    suspend fun onBookmarksRemoved(eventIds: LongArray) {
        if (eventIds.isEmpty() || !isNotificationsEnabled()) return

        queueMutex.withLock {
            // Cancel matching alarms, might they exist or not
            for (eventId in eventIds) {
                alarmManager.cancel(getAlarmPendingIntent(eventId))
            }
        }
    }

    suspend fun notifyEvent(eventId: Long) {
        scheduleDao.getEvent(eventId)?.let { event ->
            NotificationManagerCompat.from(context)
                .notify(eventId.toInt(), buildNotification(event))
        }
    }

    private suspend fun onEnabledChanged(isEnabled: Boolean) {
        return if (isEnabled) {
            updateAlarms()
        } else {
            disableAlarms()
        }
    }

    private suspend fun updateAlarms() {
        queueMutex.withLock {
            // Create/update all alarms
            val delay = userSettingsProvider.notificationsDelayInMillis.first()
            val now = System.currentTimeMillis()
            var hasAlarms = false
            for (info in bookmarksDao.getBookmarksAlarmInfo(Instant.EPOCH)) {
                val startTime = info.startTime
                val notificationTime =
                    if (startTime == null) -1L else startTime.toEpochMilli() - delay
                val pi = getAlarmPendingIntent(info.eventId)
                if (notificationTime < now) {
                    // Cancel pending alarms that are now scheduled in the past, if any
                    alarmManager.cancel(pi)
                } else {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager, AlarmManager.RTC_WAKEUP, notificationTime, pi
                    )
                    hasAlarms = true
                }
            }
            setAlarmReceiverEnabled(hasAlarms)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAlarms) {
                createNotificationChannel(context)
            }
        }
    }

    private suspend fun disableAlarms() {
        queueMutex.withLock {
            // Cancel alarms of every bookmark in the future
            for (info in bookmarksDao.getBookmarksAlarmInfo(Instant.now())) {
                alarmManager.cancel(getAlarmPendingIntent(info.eventId))
            }
            setAlarmReceiverEnabled(false)
        }
    }

    @SuppressLint("InlinedApi")
    private fun getAlarmPendingIntent(eventId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_NOTIFY_EVENT)
            .setData(eventId.toString().toUri())
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Allows disabling the Alarm Receiver so the app is not loaded at boot when it's not necessary.
     */
    private fun setAlarmReceiverEnabled(isEnabled: Boolean) {
        val componentName = ComponentName(context, AlarmReceiver::class.java)
        val flag =
            if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(
            componentName, flag, PackageManager.DONT_KILL_APP
        )
    }

    @SuppressLint("InlinedApi")
    private suspend fun buildNotification(event: Event): Notification {
        val eventPendingIntent = TaskStackBuilder
            .create(context)
            .addNextIntent(Intent(context, MainActivity::class.java))
            .addNextIntent(
                Intent(context, EventDetailsActivity::class.java)
                    .setData(event.id.toString().toUri())
            )
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        var defaultFlags = Notification.DEFAULT_SOUND
        val isVibrationEnabled = userSettingsProvider.isNotificationsVibrationEnabled.first()
        if (isVibrationEnabled) {
            defaultFlags = defaultFlags or Notification.DEFAULT_VIBRATE
        }

        val personsSummary = event.personsSummary
        val trackName = event.track.name
        val subTitle = event.subTitle
        val contentText: String
        val bigText: CharSequence?
        if (personsSummary.isNullOrEmpty()) {
            contentText = trackName
            bigText = subTitle
        } else {
            contentText = "$trackName - $personsSummary"
            bigText = buildSpannedString {
                if (!subTitle.isNullOrEmpty()) {
                    append(subTitle)
                    append('\n')
                }
                italic {
                    append(personsSummary)
                }
            }
        }

        val notificationColor = ContextCompat.getColor(context, R.color.light_color_primary)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_fosdem)
            .setColor(notificationColor)
            .setWhen(event.startTime?.toEpochMilli() ?: System.currentTimeMillis())
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
        if (userSettingsProvider.isNotificationsLedEnabled.first()) {
            notificationBuilder.setLights(notificationColor, 1000, 5000)
        }

        // Android Wear extensions
        val wearableExtender = NotificationCompat.WearableExtender()

        // Add an optional action button to show the room map image
        val roomName = event.roomName
        val roomImageResId = roomName?.let {
            context.resources.getIdentifier(
                roomNameToResourceName(it), "drawable", context.packageName
            )
        } ?: 0
        if (roomName != null && roomImageResId != 0) {
            // The room name is the unique Id of a RoomImageDialogActivity
            val mapIntent = Intent(context, RoomImageDialogActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(roomName.toUri())
                .putExtra(RoomImageDialogActivity.EXTRA_ROOM_NAME, roomName)
                .putExtra(RoomImageDialogActivity.EXTRA_ROOM_IMAGE_RESOURCE_ID, roomImageResId)
            val mapPendingIntent = PendingIntent.getActivity(
                context, 0, mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val mapTitle = context.getString(R.string.room_map)
            notificationBuilder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_place_white_24dp,
                    mapTitle,
                    mapPendingIntent
                )
            )
            // Use bigger action icon for wearable notification
            wearableExtender.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_place_white_wear,
                    mapTitle,
                    mapPendingIntent
                )
            )
        }

        notificationBuilder.extend(wearableExtender)
        return notificationBuilder.build()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "event_alarms"

        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createNotificationChannel(context: Context) {
            val notificationManager: NotificationManager? = context.getSystemService()
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                context.getString(R.string.notification_events_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                lightColor = ContextCompat.getColor(context, R.color.light_color_primary)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
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