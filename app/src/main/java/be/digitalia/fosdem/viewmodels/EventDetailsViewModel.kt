package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails

class EventDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val eventLiveData = MutableLiveData<Event>()

    val eventDetails: LiveData<EventDetails> = eventLiveData.switchMap { event: Event ->
        appDatabase.scheduleDao.getEventDetails(event)
    }

    fun setEvent(event: Event) {
        if (event != eventLiveData.value) {
            eventLiveData.value = event
        }
    }
}