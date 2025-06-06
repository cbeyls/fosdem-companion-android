package be.digitalia.fosdem.db

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.GlobalTypeConverters
import be.digitalia.fosdem.db.entities.Bookmark
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track

@Database(
    entities = [EventEntity::class, EventTitles::class, Person::class, EventToPerson::class,
        Attachment::class, Link::class, Track::class, Day::class, Bookmark::class],
    views = [Event::class],
    version = AppDatabase.VERSION,
    exportSchema = false
)
@TypeConverters(GlobalTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val scheduleDao: ScheduleDao
    abstract val bookmarksDao: BookmarksDao

    // Manually injected fields, used by Daos
    lateinit var dataStore: DataStore<Preferences>

    companion object {
        // Expose the database version to allow detecting migrations
        const val VERSION = 8
    }
}