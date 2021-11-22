package be.digitalia.fosdem

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDex
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.utils.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidApp
class FosdemApplication : Application() {

    // Injected for automatic initialization on app startup

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var alarmManager: AppAlarmManager

    // Preload UI State SharedPreferences for faster initial access
    @Inject
    @Named("UIState")
    lateinit var preferences: SharedPreferences

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }
}