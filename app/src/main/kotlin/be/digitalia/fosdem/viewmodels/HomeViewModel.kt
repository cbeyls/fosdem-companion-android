package be.digitalia.fosdem.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.LoadingState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Named
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@ContributesIntoMap(AppScope::class)
@ViewModelKey
class HomeViewModel(
    private val api: FosdemApi,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock,
    @param:Named("UIState") private val uiStateDataStore: DataStore<Preferences>,
) : ViewModel() {

    val downloadScheduleState: StateFlow<LoadingState<DownloadScheduleResult>>
        get() = api.downloadScheduleState

    val latestUpdateTime: Flow<Instant?>
        get() = scheduleDao.latestUpdateTime

    suspend fun getStandsUrl(): String {
        val year = scheduleDao.getYear()
        return if (year != null) FosdemUrls.getStands(year) else FosdemUrls.stands
    }

    fun startDownloadSchedule() {
        viewModelScope.launch {
            startDownloadScheduleInternal(clock.now())
        }
    }

    private suspend fun startDownloadScheduleInternal(now: Instant) {
        uiStateDataStore.edit {
            it[LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY] = scheduleDao.databaseVersion
            it[LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY] = now.toEpochMilliseconds()
        }

        api.startDownloadSchedule()
    }

    /**
     * Start an automatic download of the schedule data if the current data is stale.
     */
    suspend fun startDownloadScheduleIfStale(): Boolean {
        val now = clock.now()
        val latestUpdateTime = scheduleDao.latestUpdateTime.first()
        if (latestUpdateTime == null || now > latestUpdateTime + DATABASE_VALIDITY_DURATION) {
            val prefs = uiStateDataStore.data.first()
            val latestAttemptVersion = prefs[LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY] ?: 0
            val latestAttemptTime = Instant.fromEpochMilliseconds(prefs[LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY] ?: 0L)
            val isDatabaseVersionChanged = latestAttemptVersion != scheduleDao.databaseVersion
            if (isDatabaseVersionChanged || now > latestAttemptTime + AUTO_UPDATE_SNOOZE_DURATION) {
                // Try to update immediately. If it fails, the user gets a message and a retry button.
                startDownloadScheduleInternal(now)
            }
            return true
        }
        return false
    }

    fun consumeDownloadScheduleResult() {
        api.consumeDownloadScheduleResult()
    }

    companion object {
        private val DATABASE_VALIDITY_DURATION = 1.days
        private val AUTO_UPDATE_SNOOZE_DURATION = 1.days
        private val LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY = intPreferencesKey("latest_update_attempt_version")
        private val LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY = longPreferencesKey("latest_update_attempt_time")
    }
}
