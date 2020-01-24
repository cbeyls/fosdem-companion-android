package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent

class PersonInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val personLiveData = MutableLiveData<Person>()

    val events: LiveData<PagedList<StatusEvent>> = personLiveData.switchMap { person: Person ->
        appDatabase.scheduleDao.getEvents(person).toLiveData(20)
    }

    fun setPerson(person: Person) {
        if (person != personLiveData.value) {
            personLiveData.value = person
        }
    }
}