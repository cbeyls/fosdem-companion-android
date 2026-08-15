package be.digitalia.fosdem.theme

import androidx.appcompat.app.AppCompatDelegate
import be.digitalia.fosdem.settings.UserSettingsProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn

/**
 * Automatically switches the light/dark theme when the user updates its preferences.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class AndroidThemeManager(private val userSettingsProvider: UserSettingsProvider): ThemeManager {
    override suspend fun monitorUserSettings() {
        userSettingsProvider.theme.collect { theme ->
            AppCompatDelegate.setDefaultNightMode(theme ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
