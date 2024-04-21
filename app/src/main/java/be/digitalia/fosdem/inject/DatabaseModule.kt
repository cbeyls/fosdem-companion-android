package be.digitalia.fosdem.inject

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.db.entities.EventEntity
import be.digitalia.fosdem.db.entities.EventTitles
import be.digitalia.fosdem.db.entities.EventToPerson
import be.digitalia.fosdem.flow.DeferredReadDataStore
import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DB_FILE = "fosdem.sqlite"
    private const val DB_DATASTORE_FILE = "database"

    @Provides
    @Named("Database")
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(DB_DATASTORE_FILE)
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        @Named("Database") dataStore: DataStore<Preferences>
    ): AppDatabase {
        val migration3to5 = Migration(3, 5) { db ->
            with(db) {
                // Create table attachments
                execSQL("CREATE TABLE IF NOT EXISTS ${Attachment.TABLE_NAME} (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event_id` INTEGER NOT NULL, `url` TEXT NOT NULL, `description` TEXT)")
                execSQL("CREATE INDEX IF NOT EXISTS `attachment_event_id_idx` ON ${Attachment.TABLE_NAME} (`event_id`)")

                // Recreate table days with new columns
                execSQL("DROP TABLE IF EXISTS ${Day.TABLE_NAME}")
                execSQL("CREATE TABLE IF NOT EXISTS ${Day.TABLE_NAME} (`index` INTEGER NOT NULL, `date` INTEGER NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER NOT NULL, PRIMARY KEY(`index`))")
            }
        }
        val migration5to6 = Migration(5, 6) {
            // empty because it's identical to migration6to7
        }
        val migration6to7 = Migration(6, 7) { db ->
            with(db) {
                // Clear schedule (but keep bookmarks)
                execSQL("DELETE FROM ${EventTitles.TABLE_NAME}")
                execSQL("DELETE FROM ${Person.TABLE_NAME}")
                execSQL("DELETE FROM ${EventToPerson.TABLE_NAME}")
                execSQL("DELETE FROM ${Attachment.TABLE_NAME}")
                execSQL("DELETE FROM ${Link.TABLE_NAME}")
                execSQL("DELETE FROM ${Track.TABLE_NAME}")
                execSQL("DELETE FROM ${Day.TABLE_NAME}")

                // Recreate table events with new columns
                execSQL("DROP TABLE IF EXISTS ${EventEntity.TABLE_NAME}")
                execSQL("CREATE TABLE IF NOT EXISTS ${EventEntity.TABLE_NAME} (`id` INTEGER NOT NULL, `day_index` INTEGER NOT NULL, `start_time` INTEGER, `start_time_offset` INTEGER, `end_time` INTEGER, `room_name` TEXT, `url` TEXT, `track_id` INTEGER NOT NULL, `abstract` TEXT, `description` TEXT, `feedback_url` TEXT, PRIMARY KEY(`id`))")
                execSQL("CREATE INDEX IF NOT EXISTS `event_day_index_idx` ON ${EventEntity.TABLE_NAME} (`day_index`)")
                execSQL("CREATE INDEX IF NOT EXISTS `event_start_time_idx` ON ${EventEntity.TABLE_NAME} (`start_time`)")
                execSQL("CREATE INDEX IF NOT EXISTS `event_end_time_idx` ON ${EventEntity.TABLE_NAME} (`end_time`)")
                execSQL("CREATE INDEX IF NOT EXISTS `event_track_id_idx` ON ${EventEntity.TABLE_NAME} (`track_id`)")
            }
            runBlocking {
                dataStore.edit { it.clear() }
            }
        }

        val onDatabaseOpen = CompletableDeferred<Unit>()

        return Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(migration3to5, migration5to6, migration6to7)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    onDatabaseOpen.complete(Unit)
                }

                @WorkerThread
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    runBlocking {
                        dataStore.edit { it.clear() }
                    }
                }
            })
            .build()
            .also {
                // Manual dependency injection
                it.dataStore = DeferredReadDataStore(dataStore, onDatabaseOpen)
            }
    }

    @Provides
    fun provideScheduleDao(appDatabase: AppDatabase): ScheduleDao = appDatabase.scheduleDao

    @Provides
    fun provideBookmarksDao(appDatabase: AppDatabase): BookmarksDao = appDatabase.bookmarksDao
}