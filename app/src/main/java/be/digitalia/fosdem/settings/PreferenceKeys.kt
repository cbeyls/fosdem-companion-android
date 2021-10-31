package be.digitalia.fosdem.settings

object PreferenceKeys {
    const val THEME = "theme"
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"
    // Android >= O only
    const val NOTIFICATIONS_CHANNEL = "notifications_channel"
    // Android < O only
    const val NOTIFICATIONS_VIBRATE = "notifications_vibrate"
    // Android < O only
    const val NOTIFICATIONS_LED = "notifications_led"
    const val NOTIFICATIONS_DELAY = "notifications_delay"
    const val ABOUT = "about"
    const val VERSION = "version"
}