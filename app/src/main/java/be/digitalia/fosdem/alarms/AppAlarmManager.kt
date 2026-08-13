package be.digitalia.fosdem.alarms

import be.digitalia.fosdem.model.AlarmInfo

/**
 * Platform-independent abstraction to notify the AlarmManager of changes in the bookmarks data.
 */
interface AppAlarmManager {
    suspend fun onScheduleRefreshed()
    suspend fun onBookmarksAdded(alarmInfos: List<AlarmInfo>)
    suspend fun onBookmarksRemoved(eventIds: LongArray)
}
