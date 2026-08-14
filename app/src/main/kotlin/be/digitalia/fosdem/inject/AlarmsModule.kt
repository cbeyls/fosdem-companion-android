package be.digitalia.fosdem.inject

import be.digitalia.fosdem.alarms.AndroidAlarmManager
import be.digitalia.fosdem.alarms.AppAlarmManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AlarmsModule {
    @Binds
    fun provideAppAlarmManager(alarmManager: AndroidAlarmManager): AppAlarmManager
}
