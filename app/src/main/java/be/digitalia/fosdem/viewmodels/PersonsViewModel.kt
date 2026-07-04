package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.paging.toAutoCloseable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PersonsViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    val persons: Flow<PagingData<Person>> = Pager(PagingConfig(20)) {
        scheduleDao.getPersons()
            .also { addCloseable("persons", it.toAutoCloseable()) }
    }.flow.cachedIn(viewModelScope)
}