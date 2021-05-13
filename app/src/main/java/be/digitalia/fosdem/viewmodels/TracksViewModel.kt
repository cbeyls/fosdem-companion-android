package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    private val dayLiveData = MutableLiveData<Day>()

    val tracks: LiveData<List<Track>> = dayLiveData.switchMap { day: Day ->
        scheduleDao.getTracks(day)
    }

    fun setDay(day: Day) {
        if (day != dayLiveData.value) {
            dayLiveData.value = day
        }
    }
}