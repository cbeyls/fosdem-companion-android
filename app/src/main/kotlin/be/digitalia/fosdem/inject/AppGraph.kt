package be.digitalia.fosdem.inject

import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.theme.ThemeManager
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Platform-independent dependency graph
 */
interface AppGraph : ViewModelGraph {
    val themeManager: ThemeManager
    val alarmManager: AppAlarmManager
}
