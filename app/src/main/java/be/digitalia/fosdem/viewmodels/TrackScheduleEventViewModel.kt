package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TrackScheduleEventViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val dayTrackLiveData = MutableLiveData<Pair<Day, Track>>()

    val scheduleSnapshot: LiveData<List<Event>> = dayTrackLiveData.switchMap { (day, track) ->
        liveData {
            emit(scheduleDao.getEventsSnapshot(day, track))
        }
    }

    fun setDayAndTrack(day: Day, track: Track) {
        val dayTrack = day to track
        if (dayTrack != dayTrackLiveData.value) {
            dayTrackLiveData.value = dayTrack
        }
    }
}