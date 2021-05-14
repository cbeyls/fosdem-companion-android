package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PersonInfoViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val personLiveData = MutableLiveData<Person>()

    val events: LiveData<PagedList<StatusEvent>> = personLiveData.switchMap { person: Person ->
        scheduleDao.getEvents(person).toLiveData(20)
    }

    fun setPerson(person: Person) {
        if (person != personLiveData.value) {
            personLiveData.value = person
        }
    }
}