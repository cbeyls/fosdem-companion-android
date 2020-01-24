package be.digitalia.fosdem.viewmodels

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.livedata.LiveDataFactory
import be.digitalia.fosdem.model.StatusEvent
import java.util.concurrent.TimeUnit

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getInstance(application)
    private val heartbeat = LiveDataFactory.interval(1L, TimeUnit.MINUTES)

    val nextEvents: LiveData<PagedList<StatusEvent>> = heartbeat.switchMap {
        val now = System.currentTimeMillis()
        appDatabase.scheduleDao.getEventsWithStartTime(now, now + NEXT_EVENTS_INTERVAL).toLiveData(20)
    }

    val eventsInProgress: LiveData<PagedList<StatusEvent>> = heartbeat.switchMap {
        appDatabase.scheduleDao.getEventsInProgress(System.currentTimeMillis()).toLiveData(20)
    }

    companion object {
        private const val NEXT_EVENTS_INTERVAL = 30L * DateUtils.MINUTE_IN_MILLIS
    }
}