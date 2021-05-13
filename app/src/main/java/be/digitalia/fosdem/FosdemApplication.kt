package be.digitalia.fosdem

import android.app.Application
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.utils.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FosdemApplication : Application() {

    @Inject
    lateinit var alarmManager: FosdemAlarmManager

    override fun onCreate() {
        super.onCreate()

        // Initialize settings
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        // Light/Dark theme switch (requires settings)
        ThemeManager.init(this)
        // Alarms (requires settings)
        alarmManager.init()
    }

}