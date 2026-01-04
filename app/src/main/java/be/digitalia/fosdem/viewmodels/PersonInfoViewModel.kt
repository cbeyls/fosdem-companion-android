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
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.StatusEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@HiltViewModel(assistedFactory = PersonInfoViewModel.Factory::class)
class PersonInfoViewModel @AssistedInject constructor(
    scheduleDao: ScheduleDao,
    @Assisted person: Person
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
    }.flow.cachedIn(viewModelScope)

    data class PersonInfo(
        val detailsUrl: String?,
        val biography: String?
    )

    @AssistedFactory
    interface Factory {
        fun create(person: Person): PersonInfoViewModel
    }
}