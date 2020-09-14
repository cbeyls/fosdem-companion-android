package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.BookmarkStatus
import be.digitalia.fosdem.model.Event

class BookmarkStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val eventLiveData = MutableLiveData<Event?>()
    private var firstResultReceived = false

    val bookmarkStatus: LiveData<BookmarkStatus?> = eventLiveData.switchMap { event ->
        if (event == null) {
            MutableLiveData(null)
        } else {
            appDatabase.bookmarksDao.getBookmarkStatus(event)
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
                appDatabase.bookmarksDao.removeBookmarkAsync(event)
            } else {
                appDatabase.bookmarksDao.addBookmarkAsync(event)
            }
        }
    }
}