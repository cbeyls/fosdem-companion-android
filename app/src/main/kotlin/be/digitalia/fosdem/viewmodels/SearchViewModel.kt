package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.paging.toAutoCloseable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@ContributesIntoMap(AppScope::class)
@ViewModelKey
class SearchViewModel(scheduleDao: ScheduleDao) : ViewModel() {

    sealed class QueryState {
        data object Idle : QueryState()
        data object TooShort : QueryState()
        data class Valid(val query: String) : QueryState()
    }

    private val queryState = MutableStateFlow<QueryState>(QueryState.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<StatusEvent>> = queryState.flatMapLatest { queryState ->
        if (queryState is QueryState.Valid) {
            Pager(PagingConfig(20)) {
                scheduleDao.getSearchResults(queryState.query)
                    .also { addCloseable("results", it.toAutoCloseable()) }
            }.flow
        } else {
            flowOf(PagingData.empty(EMPTY_REFRESH_LOAD_STATES))
        }
    }.cachedIn(viewModelScope)

    fun setQuery(query: String) {
        queryState.value = if (query.length < SEARCH_QUERY_MIN_LENGTH) QueryState.TooShort
        else QueryState.Valid(query)
    }

    companion object {
        const val SEARCH_QUERY_MIN_LENGTH = 3

        private val EMPTY_REFRESH_LOAD_STATES = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true)
        )
    }
}
