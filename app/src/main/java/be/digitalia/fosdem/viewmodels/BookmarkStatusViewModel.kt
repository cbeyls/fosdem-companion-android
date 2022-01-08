package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.model.BookmarkStatus
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkStatusViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    private val alarmManager: AppAlarmManager
) : ViewModel() {

    private val eventLiveData = MutableLiveData<Event?>()
    private var firstResultReceived = false

    val bookmarkStatus: LiveData<BookmarkStatus?> = eventLiveData.switchMap { event ->
        if (event == null) {
            MutableLiveData(null)
        } else {
            bookmarksDao.getBookmarkStatus(event)
                .distinctUntilChanged() // Prevent updating the UI when a bookmark is added back or removed back
                .map { isBookmarked ->
                    val isUpdate = firstResultReceived
                    firstResultReceived = true
                    BookmarkStatus(isBookmarked, isUpdate)
                }
        }
    }

    var event: Event?
        get() = eventLiveData.value
        set(value) {
            if (value != eventLiveData.value) {
                firstResultReceived = false
                eventLiveData.value = value
            }
        }

    fun toggleBookmarkStatus() {
        val event = event
        val currentStatus = bookmarkStatus.value
        // Ignore the action if the status for the current event hasn't been received yet
        if (event != null && currentStatus != null && firstResultReceived) {
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