package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class EventDetailsViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted event: Event
) : ViewModel() {

    val eventDetails: LiveData<EventDetails> = scheduleDao.getEventDetails(event)

    @AssistedFactory
    interface Factory {
        fun create(event: Event): EventDetailsViewModel
    }
}