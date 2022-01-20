package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class EventViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted eventId: Long
) : ViewModel() {

    val event: LiveData<Event?> = liveData {
        emit(scheduleDao.getEvent(eventId))
    }

    @AssistedFactory
    interface Factory {
        fun create(eventId: Long): EventViewModel
    }
}