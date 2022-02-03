package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.Day
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(scheduleDao: ScheduleDao) : ViewModel() {

    val days: Flow<List<Day>> = scheduleDao.days
}