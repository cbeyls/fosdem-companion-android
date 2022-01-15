package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val queryLiveData = MutableLiveData<String>()

    sealed class Result {
        object QueryTooShort : Result()
        class Success(val list: PagedList<StatusEvent>) : Result()
    }

    val results: LiveData<Result> = queryLiveData.switchMap { query ->
        if (query.length < SEARCH_QUERY_MIN_LENGTH) {
            MutableLiveData(Result.QueryTooShort)
        } else {
            scheduleDao.getSearchResults(query)
                .toLiveData(20)
                .map { pagedList -> Result.Success(pagedList) }
        }
    }

    fun setQuery(query: String) {
        if (query != queryLiveData.value) {
            queryLiveData.value = query
        }
    }

    companion object {
        private const val SEARCH_QUERY_MIN_LENGTH = 3
    }
}