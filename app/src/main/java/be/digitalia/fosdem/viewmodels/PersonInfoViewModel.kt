package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class PersonInfoViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted person: Person
) : ViewModel() {

    val events: LiveData<PagedList<StatusEvent>> = scheduleDao.getEvents(person).toLiveData(20)

    @AssistedFactory
    interface Factory {
        fun create(person: Person): PersonInfoViewModel
    }
}