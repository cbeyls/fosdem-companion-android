package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.*
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.model.Track
import java.util.concurrent.TimeUnit

class TrackScheduleListViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val dayTrackLiveData = MutableLiveData<Pair<Day, Track>>()

    val schedule: LiveData<List<StatusEvent>> = dayTrackLiveData.switchMap { (day, track) ->
        appDatabase.scheduleDao.getEvents(day, track)
    }

    /**
     * @return The current time during the target day, or -1 outside of the target day.
     */
    val currentTime: LiveData<Long> = dayTrackLiveData
            .switchMap { (day, _) ->
                // Auto refresh during the day passed as argument
                val dayStart = day.date.time
                LiveDataFactory.scheduler(dayStart, dayStart + DateUtils.DAY_IN_MILLIS)
            }
            .switchMap { isOn ->
                if (isOn) {
                    LiveDataFactory.interval(REFRESH_TIME_INTERVAL, TimeUnit.MILLISECONDS).map {
                        System.currentTimeMillis()
                    }
                } else {
                    MutableLiveData(-1L)
                }
            }

    fun setDayAndTrack(day: Day, track: Track) {
        val dayTrack = day to track
        if (dayTrack != dayTrackLiveData.value) {
            dayTrackLiveData.value = dayTrack
        }
    }

    companion object {
        private const val REFRESH_TIME_INTERVAL = DateUtils.MINUTE_IN_MILLIS
    }
}