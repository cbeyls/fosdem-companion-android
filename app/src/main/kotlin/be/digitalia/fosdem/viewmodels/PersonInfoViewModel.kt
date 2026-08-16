package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.flow.stateFlow
import be.digitalia.fosdem.flow.versionedResourceFlow
import be.digitalia.fosdem.inject.CallbackViewModelAssistedFactory
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.paging.toAutoCloseable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@AssistedInject
class PersonInfoViewModel(
    @Assisted person: Person,
    scheduleDao: ScheduleDao,
) : ViewModel() {

    val personInfo: Flow<PersonInfo> = stateFlow(viewModelScope, null) {
        versionedResourceFlow(scheduleDao.version) {
            val personDetails = scheduleDao.getPersonDetails(person)
            PersonInfo(
                detailsUrl = personDetails?.slug?.let { slug ->
                    scheduleDao.baseUrl.first()?.let { baseUrl ->
                        FosdemUrls.getPerson(baseUrl, slug)
                    }
                },
                biography = personDetails?.biography
            )
        }
    }.filterNotNull()

    val events: Flow<PagingData<StatusEvent>> = Pager(PagingConfig(20)) {
        scheduleDao.getEvents(person)
            .also { addCloseable("events", it.toAutoCloseable()) }
    }.flow.cachedIn(viewModelScope)

    data class PersonInfo(
        val detailsUrl: String?,
        val biography: String?
    )

    @AssistedFactory
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModelAssistedFactory>())
    @ViewModelAssistedFactoryKey(PersonInfoViewModel::class)
    fun interface Factory : CallbackViewModelAssistedFactory {
        fun create(person: Person): PersonInfoViewModel
    }
}
