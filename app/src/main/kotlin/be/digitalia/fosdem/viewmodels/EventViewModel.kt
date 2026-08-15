package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.inject.CallbackViewModelAssistedFactory
import be.digitalia.fosdem.model.Event
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

@AssistedInject
class EventViewModel(
    @Assisted eventId: Long,
    scheduleDao: ScheduleDao,
) : ViewModel() {

    val event: Deferred<Event?> = viewModelScope.async {
        scheduleDao.getEvent(eventId)
    }

    @AssistedFactory
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModelAssistedFactory>())
    @ViewModelAssistedFactoryKey(EventViewModel::class)
    fun interface Factory : CallbackViewModelAssistedFactory {
        fun create(eventId: Long): EventViewModel
    }
}
