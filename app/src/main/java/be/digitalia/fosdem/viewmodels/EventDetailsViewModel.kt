package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class EventDetailsViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted event: Event
) : ViewModel() {

    val eventDetails: Deferred<EventDetails> = viewModelScope.async {
        scheduleDao.getEventDetails(event)
    }

    @AssistedFactory
    interface Factory {
        fun create(event: Event): EventDetailsViewModel
    }
}