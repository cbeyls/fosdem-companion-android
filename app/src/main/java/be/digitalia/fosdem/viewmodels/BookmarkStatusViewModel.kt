package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.BookmarkStatus
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkStatusViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val alarmManager: AppAlarmManager
) : ViewModel() {

    private val eventStateFlow = MutableStateFlow<Event?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarkStatus: StateFlow<BookmarkStatus?> =
        stateFlow(viewModelScope, null) {
            eventStateFlow.flatMapLatest { event ->
                if (event == null) {
                    flowOf(null)
                } else {
                    versionedResourceFlow(bookmarksDao.version) {
                        val isBookmarked = bookmarksDao.getBookmarkStatus(event)
                        BookmarkStatus(event.id, isBookmarked)
                    }
                }
            }
        }

    var event: Event?
        get() = eventStateFlow.value
        set(value) {
            eventStateFlow.value = value
        }

    fun toggleBookmarkStatus() {
        val event = eventStateFlow.value
        val currentStatus = bookmarkStatus.value
        // Ignore the action if the status for the current event hasn't been received yet
        if (event != null && currentStatus != null && event.id == currentStatus.eventId) {
            if (currentStatus.isBookmarked) {
                removeBookmark(event)
            } else {
                addBookmark(event)
            }
        }
    }

    private fun removeBookmark(event: Event) {
        val eventIds = longArrayOf(event.id)
        BackgroundWorkScope.launch {
            if (bookmarksDao.removeBookmarks(eventIds) > 0) {
                alarmManager.onBookmarksRemoved(eventIds)
            }
        }
    }

    private fun addBookmark(event: Event) {
        BackgroundWorkScope.launch {
            bookmarksDao.addBookmark(event)?.let { alarmInfo ->
                alarmManager.onBookmarksAdded(listOf(alarmInfo))
            }
        }
    }
}