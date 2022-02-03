package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

class PersonInfoViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted person: Person
) : ViewModel() {

    val events: Flow<PagingData<StatusEvent>> = Pager(PagingConfig(20)) {
        scheduleDao.getEvents(person)
    }.flow.cachedIn(viewModelScope)

    @AssistedFactory
    interface Factory {
        fun create(person: Person): PersonInfoViewModel
    }
}