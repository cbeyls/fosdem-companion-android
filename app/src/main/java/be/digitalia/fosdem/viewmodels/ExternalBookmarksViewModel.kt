package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.utils.BackgroundWorkScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExternalBookmarksViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    private val bookmarksDao: BookmarksDao,
    private val alarmManager: AppAlarmManager,
    @Assisted private val bookmarkIds: LongArray
) : ViewModel() {

    val bookmarks: Flow<PagingData<StatusEvent>> =
        Pager(PagingConfig(20)) {
            scheduleDao.getEvents(bookmarkIds)
        }.flow.cachedIn(viewModelScope)

    fun addAll() {
        BackgroundWorkScope.launch {
            bookmarksDao.addBookmarks(bookmarkIds).let { alarmInfos ->
                alarmManager.onBookmarksAdded(alarmInfos)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(bookmarkIds: LongArray): ExternalBookmarksViewModel
    }
}