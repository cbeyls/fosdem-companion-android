package be.digitalia.fosdem.viewmodels

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class TracksViewModel @Inject constructor(
    scheduleDao: ScheduleDao,
    @param:Named("UIState") private val uiStatePreferences: SharedPreferences,
) : ViewModel() {

    data class TracksState(
        val days: List<Day>,
        val initialPage: Int,
    )

    val state: Flow<TracksState> = flow {
        // Restore the saved current page as part of the state
        val defaultPageIndex = uiStatePreferences.getInt(TRACKS_CURRENT_PAGE_PREF_KEY, 0)
        scheduleDao.days
            .map { days ->
                TracksState(
                    days = days,
                    initialPage = defaultPageIndex.coerceAtMost(days.lastIndex),
                )
            }
            .collect(this)
    }

    fun saveCurrentPage(page: Int) {
        uiStatePreferences.edit {
            putInt(TRACKS_CURRENT_PAGE_PREF_KEY, page)
        }
    }

    companion object {
        private const val TRACKS_CURRENT_PAGE_PREF_KEY = "tracks_current_page"
    }
}
