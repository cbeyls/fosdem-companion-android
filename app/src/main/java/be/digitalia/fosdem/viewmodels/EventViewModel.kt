package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Event

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val eventIdLiveData = MutableLiveData<Long>()

    val event: LiveData<Event?> = eventIdLiveData.switchMap { id: Long ->
        MutableLiveData<Event?>().also {
            appDatabase.queryExecutor.execute {
                it.postValue(appDatabase.scheduleDao.getEvent(id))
            }
        }
    }

    val isEventIdSet = eventIdLiveData.value != null

    fun setEventId(eventId: Long) {
        if (eventId != eventIdLiveData.value) {
            eventIdLiveData.value = eventId
        }
    }
}