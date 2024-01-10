package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.SharedFlowContext
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.synchronizedTickerFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.parsers.ExportedBookmarksParser
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val scheduleDao: ScheduleDao,
    private val alarmManager: AppAlarmManager,
    private val application: Application
) : ViewModel() {

    private val hidePastEventsStateFlow = MutableStateFlow<Boolean?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<Event>?> = stateFlow(viewModelScope, null) {
        hidePastEventsStateFlow.filterNotNull().flatMapLatest { hidePastEvents ->
            if (hidePastEvents) {
                // Refresh upcoming bookmarks every 2 minutes
                synchronizedTickerFlow(REFRESH_PERIOD)
                    .flatMapLatest {
                        getObservableBookmarks(Instant.now())
                    }
            } else {
                getObservableBookmarks(Instant.EPOCH)
            }
        }
    }

    private fun SharedFlowContext.getObservableBookmarks(minEndTime: Instant): Flow<List<Event>> =
        versionedResourceFlow(bookmarksDao.version) {
            bookmarksDao.getBookmarks(minEndTime)
        }

    var hidePastEvents: Boolean
        get() = hidePastEventsStateFlow.value == true
        set(value) {
            hidePastEventsStateFlow.value = value
        }

    fun removeBookmarks(eventIds: LongArray) {
        BackgroundWorkScope.launch {
            if (bookmarksDao.removeBookmarks(eventIds) > 0) {
                alarmManager.onBookmarksRemoved(eventIds)
            }
        }
    }

    suspend fun readBookmarkIds(uri: Uri): LongArray = withContext(Dispatchers.IO) {
        val parser = ExportedBookmarksParser(
            applicationId = BuildConfig.APPLICATION_ID,
            conferenceId = scheduleDao.getYear().toString()
        )
        checkNotNull(application.contentResolver.openInputStream(uri)).source().buffer().use {
            parser.parse(it)
        }
    }

    companion object {
        private val REFRESH_PERIOD = 2.minutes
    }
}