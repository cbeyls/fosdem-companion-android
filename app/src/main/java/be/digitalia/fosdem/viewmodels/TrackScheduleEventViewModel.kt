package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Track

class TrackScheduleEventViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val dayTrackLiveData = MutableLiveData<Pair<Day, Track>>()

    val scheduleSnapshot: LiveData<List<Event>> = dayTrackLiveData.switchMap { (day, track) ->
        MutableLiveData<List<Event>>().also {
            appDatabase.queryExecutor.execute {
                it.postValue(appDatabase.scheduleDao.getEventsSnapshot(day, track))
            }
        }
    }

    fun setDayAndTrack(day: Day, track: Track) {
        val dayTrack = day to track
        if (dayTrack != dayTrackLiveData.value) {
            dayTrackLiveData.value = dayTrack
        }
    }
}