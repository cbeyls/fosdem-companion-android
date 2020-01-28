package be.digitalia.fosdem.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track

class TracksViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val dayLiveData = MutableLiveData<Day>()

    val tracks: LiveData<List<Track>> = dayLiveData.switchMap { day: Day ->
        appDatabase.scheduleDao.getTracks(day)
    }

    fun setDay(day: Day) {
        if (day != dayLiveData.value) {
            dayLiveData.value = day
        }
    }
}