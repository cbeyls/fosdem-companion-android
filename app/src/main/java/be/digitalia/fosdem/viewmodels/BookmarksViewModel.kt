package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.TimeSource

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val scheduleDao: ScheduleDao,
    private val alarmManager: AppAlarmManager,
    private val application: Application,
    timeSource: TimeSource,
    clock: Clock,
    @param:Named("UIState") private val uiStateDataStore: DataStore<Preferences>,
) : ViewModel() {

    val hidePastEvents: Flow<Boolean> =
        uiStateDataStore.data.map { it[HIDE_PAST_EVENTS_PREF_KEY] ?: false }

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<Event>?> = stateFlow(viewModelScope, null) {
        hidePastEvents.flatMapLatest { hidePastEvents ->
            if (hidePastEvents) {
                // Refresh upcoming bookmarks every 2 minutes
                synchronizedTickerFlow(REFRESH_PERIOD, timeSource)
                    .flatMapLatest {
                        getObservableBookmarks(clock.now())
                    }
            } else {
                getObservableBookmarks(BookmarksDao.EPOCH)
            }
        }
    }

    private fun SharedFlowContext.getObservableBookmarks(minEndTime: Instant): Flow<List<Event>> =
        versionedResourceFlow(bookmarksDao.version) {
            bookmarksDao.getBookmarks(minEndTime)
        }

    val isImportExportEnabled: Flow<Boolean> =
        scheduleDao.latestUpdateTime
            .map { it != null }
            .distinctUntilChanged()

    fun toggleHidePastEvents() {
        viewModelScope.launch {
            uiStateDataStore.edit {
                it[HIDE_PAST_EVENTS_PREF_KEY] = !(it[HIDE_PAST_EVENTS_PREF_KEY] ?: false)
            }
        }
    }

    fun removeBookmarks(eventIds: LongArray) {
        BackgroundWorkScope.launch {
            if (bookmarksDao.removeBookmarks(eventIds) > 0) {
                alarmManager.onBookmarksRemoved(eventIds)
            }
        }
    }

    suspend fun readBookmarkIds(uri: Uri): LongArray = withContext(Dispatchers.IO) {
        val conferenceId = scheduleDao.getYear()?.toString() ?: throw IllegalStateException("Empty database")
        val parser = ExportedBookmarksParser(
            applicationId = BuildConfig.APPLICATION_ID,
            conferenceId = conferenceId
        )
        checkNotNull(application.contentResolver.openInputStream(uri)).source().buffer().use {
            parser.parse(it)
        }
    }

    companion object {
        private val REFRESH_PERIOD = 2.minutes
        private val HIDE_PAST_EVENTS_PREF_KEY = booleanPreferencesKey("bookmarks_upcoming_only")
    }
}
