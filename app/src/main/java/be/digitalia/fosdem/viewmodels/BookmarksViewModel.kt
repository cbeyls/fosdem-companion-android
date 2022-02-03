package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.flowWhileShared
import be.digitalia.fosdem.flow.rememberTickerFlow
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.parsers.ExportedBookmarksParser
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val scheduleDao: ScheduleDao,
    private val alarmManager: AppAlarmManager,
    private val application: Application
) : ViewModel() {

    private val upcomingOnlyStateFlow = MutableStateFlow<Boolean?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<Event>?> = stateFlow(viewModelScope, null) { subscriptionCount ->
        upcomingOnlyStateFlow.filterNotNull().flatMapLatest { upcomingOnly ->
            if (upcomingOnly) {
                // Refresh upcoming bookmarks every 2 minutes
                rememberTickerFlow(REFRESH_PERIOD)
                    .flowWhileShared(subscriptionCount, SharingStarted.WhileSubscribed())
                    .flatMapLatest {
                        getObservableBookmarks(Instant.now() - TIME_OFFSET, subscriptionCount)
                    }
            } else {
                getObservableBookmarks(Instant.EPOCH, subscriptionCount)
            }
        }
    }

    private fun getObservableBookmarks(
        minStartTime: Instant,
        subscriptionCount: StateFlow<Int>
    ): Flow<List<Event>> = versionedResourceFlow(bookmarksDao.version, subscriptionCount) {
        bookmarksDao.getBookmarks(minStartTime)
    }

    var upcomingOnly: Boolean
        get() = upcomingOnlyStateFlow.value == true
        set(value) {
            upcomingOnlyStateFlow.value = value
        }

    fun removeBookmarks(eventIds: LongArray) {
        BackgroundWorkScope.launch {
            if (bookmarksDao.removeBookmarks(eventIds) > 0) {
                alarmManager.onBookmarksRemoved(eventIds)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun readBookmarkIds(uri: Uri): LongArray = withContext(Dispatchers.IO) {
        val parser = ExportedBookmarksParser(BuildConfig.APPLICATION_ID, scheduleDao.getYear())
        checkNotNull(application.contentResolver.openInputStream(uri)).source().buffer().use {
            parser.parse(it)
        }
    }

    companion object {
        private val REFRESH_PERIOD = TimeUnit.MINUTES.toMillis(2L)

        // In upcomingOnly mode, events that just started are still shown for 5 minutes
        private val TIME_OFFSET = Duration.ofMinutes(5L)
    }
}