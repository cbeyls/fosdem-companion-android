package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val eventLiveData = MutableLiveData<Event>()

    val eventDetails: LiveData<EventDetails> = eventLiveData.switchMap { event: Event ->
        scheduleDao.getEventDetails(event)
    }

    fun setEvent(event: Event) {
        if (event != eventLiveData.value) {
            eventLiveData.value = event
        }
    }
}