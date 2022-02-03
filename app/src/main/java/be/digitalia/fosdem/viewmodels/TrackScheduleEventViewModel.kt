package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class TrackScheduleEventViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day,
    @Assisted track: Track
) : ViewModel() {

    val scheduleSnapshot: Deferred<List<Event>> = viewModelScope.async {
        scheduleDao.getEventsWithoutBookmarkStatus(day, track)
    }

    @AssistedFactory
    interface Factory {
        fun create(day: Day, track: Track): TrackScheduleEventViewModel
    }
}