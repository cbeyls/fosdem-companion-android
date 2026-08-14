package be.digitalia.fosdem.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class TracksViewModel @Inject constructor(
    scheduleDao: ScheduleDao,
    @param:Named("UIState") private val uiStatePreferences: DataStore<Preferences>,
) : ViewModel() {

    data class TracksState(
        val days: List<Day>,
        val initialPage: Int,
    )

    val state: Flow<TracksState> = flow {
        // Restore the saved current page as part of the state
        val defaultPageIndex = uiStatePreferences.data.first()[TRACKS_CURRENT_PAGE_PREF_KEY] ?: 0
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
        viewModelScope.launch {
            uiStatePreferences.edit {
                it[TRACKS_CURRENT_PAGE_PREF_KEY] = page
            }
        }
    }

    companion object {
        private val TRACKS_CURRENT_PAGE_PREF_KEY = intPreferencesKey("tracks_current_page")
    }
}
