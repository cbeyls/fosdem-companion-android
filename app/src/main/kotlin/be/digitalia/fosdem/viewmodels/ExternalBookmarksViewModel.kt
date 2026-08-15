package be.digitalia.fosdem.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.inject.CallbackViewModelAssistedFactory
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AssistedInject
class ExternalBookmarksViewModel(
    @Assisted private val bookmarkIds: LongArray,
    scheduleDao: ScheduleDao,
    private val bookmarksDao: BookmarksDao,
    private val alarmManager: AppAlarmManager,
) : ViewModel() {

    val bookmarks: Flow<PagingData<StatusEvent>> =
        Pager(PagingConfig(20)) {
            scheduleDao.getEvents(bookmarkIds)
                .also { addCloseable("bookmarks", it.toAutoCloseable()) }
        }.flow.cachedIn(viewModelScope)

    fun addAll() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                bookmarksDao.addBookmarks(bookmarkIds).let { alarmInfos ->
                    alarmManager.onBookmarksAdded(alarmInfos)
                }
            }
        }
    }

    @AssistedFactory
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModelAssistedFactory>())
    @ViewModelAssistedFactoryKey(ExternalBookmarksViewModel::class)
    fun interface Factory : CallbackViewModelAssistedFactory {
        fun create(bookmarkIds: LongArray): ExternalBookmarksViewModel
    }
}
