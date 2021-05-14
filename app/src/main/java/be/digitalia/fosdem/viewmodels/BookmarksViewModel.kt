package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.net.Uri
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.parsers.ExportedBookmarksParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val scheduleDao: ScheduleDao,
    private val application: Application
) : ViewModel() {

    private val upcomingOnlyLiveData = MutableLiveData<Boolean>()

    val bookmarks: LiveData<List<Event>> = upcomingOnlyLiveData.switchMap { upcomingOnly: Boolean ->
        if (upcomingOnly) {
            // Refresh upcoming bookmarks every 2 minutes
            LiveDataFactory.interval(2L, TimeUnit.MINUTES)
                .switchMap {
                    bookmarksDao.getBookmarks(System.currentTimeMillis() - TIME_OFFSET)
                }
        } else {
            bookmarksDao.getBookmarks(-1L)
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
        bookmarksDao.removeBookmarksAsync(eventIds)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun readBookmarkIds(uri: Uri): LongArray = withContext(Dispatchers.IO) {
        val parser = ExportedBookmarksParser(BuildConfig.APPLICATION_ID, scheduleDao.getYear())
        checkNotNull(application.contentResolver.openInputStream(uri)).source().buffer().use {
            parser.parse(it)
        }
    }

    companion object {
        // In upcomingOnly mode, events that just started are still shown for 5 minutes
        private const val TIME_OFFSET = 5L * DateUtils.MINUTE_IN_MILLIS
    }
}