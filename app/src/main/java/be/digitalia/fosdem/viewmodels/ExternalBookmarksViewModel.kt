package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExternalBookmarksViewModel @Inject constructor(
    scheduleDao: ScheduleDao,
    private val bookmarksDao: BookmarksDao,
    private val alarmManager: AppAlarmManager
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
        BackgroundWorkScope.launch {
            bookmarksDao.addBookmarks(bookmarkIds).let { alarmInfos ->
                alarmManager.onBookmarksAdded(alarmInfos)
            }
        }
    }
}