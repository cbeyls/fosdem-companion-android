package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class BookmarksCalendarViewModel @Inject constructor(
    private val bookmarksDao: BookmarksDao,
    scheduleDao: ScheduleDao
) : ViewModel() {

    val days: Flow<List<Day>> = scheduleDao.days

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarksByDay: StateFlow<Map<Int, List<Event>>?> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(bookmarksDao.version) {
            bookmarksDao.getBookmarks()
        }.flatMapLatest { bookmarks ->
            // Group bookmarks by day index
            flowOf(bookmarks.groupBy { it.day.index })
        }
    }
}
