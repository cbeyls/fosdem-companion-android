package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.net.Uri
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.parsers.ExportedBookmarksParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.util.concurrent.TimeUnit

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val upcomingOnlyLiveData = MutableLiveData<Boolean>()

    val bookmarks: LiveData<List<Event>> = upcomingOnlyLiveData.switchMap { upcomingOnly: Boolean ->
        if (upcomingOnly) {
            // Refresh upcoming bookmarks every 2 minutes
            LiveDataFactory.interval(2L, TimeUnit.MINUTES)
                    .switchMap {
                        appDatabase.bookmarksDao.getBookmarks(System.currentTimeMillis() - TIME_OFFSET)
                    }
        } else {
            appDatabase.bookmarksDao.getBookmarks(-1L)
        }
    }

    var upcomingOnly: Boolean
        get() = upcomingOnlyLiveData.value == true
        set(value) {
            if (value != upcomingOnlyLiveData.value) {
                upcomingOnlyLiveData.value = value
            }
        }

    fun removeBookmarks(eventIds: LongArray) {
        appDatabase.bookmarksDao.removeBookmarksAsync(eventIds)
    }

    suspend fun readBookmarkIds(uri: Uri): LongArray {
        return withContext(Dispatchers.IO) {
            val parser = ExportedBookmarksParser(BuildConfig.APPLICATION_ID, appDatabase.scheduleDao.getYear())
            checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).source().buffer().use {
                parser.parse(it)
            }
        }
    }

    companion object {
        // In upcomingOnly mode, events that just started are still shown for 5 minutes
        private const val TIME_OFFSET = 5L * DateUtils.MINUTE_IN_MILLIS
    }
}