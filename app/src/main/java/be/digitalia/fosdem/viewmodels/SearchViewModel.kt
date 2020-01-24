package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.StatusEvent
import kotlin.contracts.ExperimentalContracts

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val queryLiveData = MutableLiveData<String>()

    sealed class Result {
        object QueryTooShort : Result()
        class Success(val list: PagedList<StatusEvent>) : Result()
    }

    @ExperimentalContracts
    val results: LiveData<Result> = queryLiveData.switchMap<String?, Result> { query ->
        if (query == null || query.length < SEARCH_QUERY_MIN_LENGTH) {
            MutableLiveData(Result.QueryTooShort)
        } else {
            appDatabase.scheduleDao.getSearchResults(query)
                    .toLiveData(20)
                    .map { pagedList -> Result.Success(pagedList) }
        }
    }

    var query: String?
        get() = queryLiveData.value
        set(value) {
            if (value != queryLiveData.value) {
                queryLiveData.value = value
            }
        }

    companion object {
        private const val SEARCH_QUERY_MIN_LENGTH = 3
    }
}