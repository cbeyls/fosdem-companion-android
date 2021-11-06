package be.digitalia.fosdem.inject

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DB_FILE = "fosdem.sqlite"
    private const val DB_PREFS_FILE = "database"

    @Provides
    @Named("Database")
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(DB_PREFS_FILE, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context,
                           @Named("Database") sharedPreferences: SharedPreferences,
                           alarmManager: AppAlarmManager): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    @WorkerThread
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        sharedPreferences.edit().clear().commit()
                    }
                })
                .build()
                .also {
                    // Manual dependency injection
                    it.sharedPreferences = sharedPreferences
                    it.alarmManager = alarmManager
                }
    }

    @Provides
    fun provideScheduleDao(appDatabase: AppDatabase): ScheduleDao = appDatabase.scheduleDao

    @Provides
    fun provideBookmarksDao(appDatabase: AppDatabase): BookmarksDao = appDatabase.bookmarksDao
}