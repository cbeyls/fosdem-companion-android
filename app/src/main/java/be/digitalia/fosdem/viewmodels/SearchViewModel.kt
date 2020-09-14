package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.StatusEvent

class SearchViewModel(application: Application, private val state: SavedStateHandle) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val queryLiveData: LiveData<String> = state.getLiveData(STATE_QUERY)

    sealed class Result {
        object QueryTooShort : Result()
        class Success(val list: PagedList<StatusEvent>) : Result()
    }

    val results: LiveData<Result> = queryLiveData.switchMap { query ->
        if (query.length < SEARCH_QUERY_MIN_LENGTH) {
            MutableLiveData(Result.QueryTooShort)
        } else {
            appDatabase.scheduleDao.getSearchResults(query)
                    .toLiveData(20)
                    .map { pagedList -> Result.Success(pagedList) }
        }
    }

    var query: String
        get() = queryLiveData.value ?: ""
        set(value) {
            if (value != queryLiveData.value) {
                state[STATE_QUERY] = value
            }
        }

    companion object {
        private const val SEARCH_QUERY_MIN_LENGTH = 3
        private const val STATE_QUERY = "query"
    }
}