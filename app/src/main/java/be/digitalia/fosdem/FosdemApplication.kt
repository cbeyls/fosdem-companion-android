package be.digitalia.fosdem

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.utils.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FosdemApplication : Application() {

    // Injected for automatic initialization on app startup
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var alarmManager: AppAlarmManager

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }
}