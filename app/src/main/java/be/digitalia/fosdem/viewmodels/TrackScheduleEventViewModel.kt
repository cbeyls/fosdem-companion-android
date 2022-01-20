package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class TrackScheduleEventViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day,
    @Assisted track: Track
) : ViewModel() {

    val scheduleSnapshot: LiveData<List<Event>> = liveData {
        emit(scheduleDao.getEventsSnapshot(day, track))
    }

    @AssistedFactory
    interface Factory {
        fun create(day: Day, track: Track): TrackScheduleEventViewModel
    }
}