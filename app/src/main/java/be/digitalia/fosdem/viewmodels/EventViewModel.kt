package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val eventIdLiveData = MutableLiveData<Long>()

    val event: LiveData<Event?> = eventIdLiveData.switchMap { id ->
        liveData {
            emit(scheduleDao.getEvent(id))
        }
    }

    val isEventIdSet
        get() = eventIdLiveData.value != null

    fun setEventId(eventId: Long) {
        if (eventId != eventIdLiveData.value) {
            eventIdLiveData.value = eventId
        }
    }
}