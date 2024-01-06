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
import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(DB_DATASTORE_FILE) }
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        @Named("Database") dataStore: DataStore<Preferences>
    ): AppDatabase {
        val migration3to5 = Migration(3, 5) { db ->
            with(db) {
                // Clear schedule (but keep bookmarks)
                execSQL("DELETE FROM events")
                execSQL("DELETE FROM events_titles")
                execSQL("DELETE FROM persons")
                execSQL("DELETE FROM events_persons")
                execSQL("DELETE FROM links")
                execSQL("DELETE FROM tracks")

                // Create table attachments
                execSQL("CREATE TABLE IF NOT EXISTS ${Attachment.TABLE_NAME} (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event_id` INTEGER NOT NULL, `url` TEXT NOT NULL, `description` TEXT)")
                execSQL("CREATE INDEX IF NOT EXISTS `attachment_event_id_idx` ON ${Attachment.TABLE_NAME} (`event_id`)")

                // Recreate table days with new columns
                execSQL("DROP TABLE IF EXISTS ${Day.TABLE_NAME}")
                execSQL("CREATE TABLE IF NOT EXISTS ${Day.TABLE_NAME} (`index` INTEGER NOT NULL, `date` INTEGER NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER NOT NULL, PRIMARY KEY(`index`))")
            }
            runBlocking {
                dataStore.edit { it.clear() }
            }
        }

        return Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(migration3to5)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
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
                it.dataStore = dataStore
            }
    }

    @Provides
    fun provideScheduleDao(appDatabase: AppDatabase): ScheduleDao = appDatabase.scheduleDao

    @Provides
    fun provideBookmarksDao(appDatabase: AppDatabase): BookmarksDao = appDatabase.bookmarksDao
}