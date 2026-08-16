package be.digitalia.fosdem.theme

interface ThemeManager {
    suspend fun monitorUserSettings()
}
