package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExternalBookmarksViewModel @Inject constructor(
    scheduleDao: ScheduleDao,
    private val bookmarksDao: BookmarksDao
) : ViewModel() {

    private val bookmarkIdsLiveData = MutableLiveData<LongArray>()

    val bookmarks: LiveData<PagedList<StatusEvent>> = bookmarkIdsLiveData.switchMap { bookmarkIds ->
        scheduleDao.getEvents(bookmarkIds).toLiveData(20)
    }

    fun setBookmarkIds(bookmarkIds: LongArray) {
        val value = bookmarkIdsLiveData.value
        if (value == null || !bookmarkIds.contentEquals(value)) {
            bookmarkIdsLiveData.value = bookmarkIds
        }
    }

    fun addAll() {
        val bookmarkIds = bookmarkIdsLiveData.value ?: return
        bookmarksDao.addBookmarksAsync(bookmarkIds)
    }
}