package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Event
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class EventViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted eventId: Long
) : ViewModel() {

    val event: Deferred<Event?> = viewModelScope.async {
        scheduleDao.getEvent(eventId)
    }

    @AssistedFactory
    interface Factory {
        fun create(eventId: Long): EventViewModel
    }
}