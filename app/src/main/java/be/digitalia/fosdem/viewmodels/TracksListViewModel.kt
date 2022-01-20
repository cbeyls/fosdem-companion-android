package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class TracksListViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted day: Day
) : ViewModel() {

    val tracks: LiveData<List<Track>> = scheduleDao.getTracks(day)

    @AssistedFactory
    interface Factory {
        fun create(day: Day): TracksListViewModel
    }
}