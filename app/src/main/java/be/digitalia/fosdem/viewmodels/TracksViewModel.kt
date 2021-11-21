package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    val days: LiveData<List<Day>> = scheduleDao.days
        .asLiveData(viewModelScope.coroutineContext)
        .distinctUntilChanged()
}