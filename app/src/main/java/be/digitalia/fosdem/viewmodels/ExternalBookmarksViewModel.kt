package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.StatusEvent

class ExternalBookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val bookmarkIdsLiveData = MutableLiveData<LongArray>()

    val bookmarks: LiveData<PagedList<StatusEvent>> = bookmarkIdsLiveData.switchMap { bookmarkIds: LongArray ->
        appDatabase.scheduleDao.getEvents(bookmarkIds).toLiveData(20)
    }

    fun setBookmarkIds(bookmarkIds: LongArray) {
        val value = bookmarkIdsLiveData.value
        if (value == null || !bookmarkIds.contentEquals(value)) {
            bookmarkIdsLiveData.value = bookmarkIds
        }
    }

    fun addAll() {
        val bookmarkIds = bookmarkIdsLiveData.value ?: return
        appDatabase.bookmarksDao.addBookmarksAsync(bookmarkIds)
    }
}