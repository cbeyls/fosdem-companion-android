package be.digitalia.fosdem.db

import android.content.SharedPreferences
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import be.digitalia.fosdem.alarms.FosdemAlarmManager
import be.digitalia.fosdem.db.converters.GlobalTypeConverters
import be.digitalia.fosdem.db.entities.Bookmark
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Database(
    entities = [EventEntity::class, EventTitles::class, Person::class, EventToPerson::class,
        Link::class, Track::class, Day::class, Bookmark::class], version = 2, exportSchema = false
)
@TypeConverters(GlobalTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val scheduleDao: ScheduleDao
    abstract val bookmarksDao: BookmarksDao

    lateinit var sharedPreferences: SharedPreferences
        private set
    lateinit var alarmManager: FosdemAlarmManager
        private set

    override fun init(configuration: DatabaseConfiguration) {
        super.init(configuration)
        // Manual dependency injection
        val entryPoint = EntryPointAccessors.fromApplication(configuration.context, AppDatabaseEntryPoint::class.java)
        sharedPreferences = entryPoint.sharedPreferences
        alarmManager = entryPoint.alarmManager
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppDatabaseEntryPoint {
        @get:Named("Database")
        val sharedPreferences: SharedPreferences
        val alarmManager: FosdemAlarmManager
    }
}