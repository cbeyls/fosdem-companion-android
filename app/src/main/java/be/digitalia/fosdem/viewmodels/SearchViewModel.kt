package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    sealed class QueryState {
        object Idle : QueryState()
        object TooShort : QueryState()
        data class Valid(val query: String) : QueryState()
    }

    private val queryState = MutableStateFlow<QueryState>(QueryState.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<StatusEvent>> = queryState.flatMapLatest { queryState ->
        if (queryState is QueryState.Valid) {
            Pager(PagingConfig(20)) {
                scheduleDao.getSearchResults(queryState.query)
            }.flow
        } else {
            flowOf(PagingData.empty())
        }
    }.cachedIn(viewModelScope)

    fun setQuery(query: String) {
        queryState.value = if (query.length < SEARCH_QUERY_MIN_LENGTH) QueryState.TooShort
        else QueryState.Valid(query)
    }

    companion object {
        const val SEARCH_QUERY_MIN_LENGTH = 3
    }
}