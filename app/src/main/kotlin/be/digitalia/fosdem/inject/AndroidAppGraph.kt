package be.digitalia.fosdem.inject

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentFactory
import be.digitalia.fosdem.alarms.AndroidAlarmManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph, AndroidInjectors {

    val fragmentFactory: FragmentFactory
    override val alarmManager: AndroidAlarmManager

    @Binds
    val Application.bind: Context

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AndroidAppGraph
    }
}
