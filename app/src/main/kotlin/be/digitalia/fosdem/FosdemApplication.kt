package be.digitalia.fosdem

import android.app.Application
import be.digitalia.fosdem.inject.AndroidAppGraph
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.launch

class FosdemApplication : Application() {

    val appGraph: AndroidAppGraph by lazy {
        createGraphFactory<AndroidAppGraph.Factory>().create(this)
    }

    override fun onCreate() {
        super.onCreate()

        val graph = appGraph
        BackgroundWorkScope.launch {
            graph.themeManager.monitorUserSettings()
        }
        BackgroundWorkScope.launch {
            graph.alarmManager.monitorUserSettings()
        }
    }
}
