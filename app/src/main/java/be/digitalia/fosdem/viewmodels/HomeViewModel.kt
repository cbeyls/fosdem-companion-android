package be.digitalia.fosdem.viewmodels

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: FosdemApi,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock,
    @param:Named("UIState") private val uiStatePreferences: SharedPreferences,
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
        startDownloadScheduleInternal(clock.now())
    }

    private fun startDownloadScheduleInternal(now: Instant) {
        uiStatePreferences.edit {
            putInt(LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY, scheduleDao.databaseVersion)
            putLong(LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY, now.toEpochMilliseconds())
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
            val latestAttemptVersion = uiStatePreferences.getInt(LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY, 0)
            val latestAttemptTime = Instant.fromEpochMilliseconds(
                uiStatePreferences.getLong(LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY, 0L)
            )
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
        private const val LATEST_UPDATE_ATTEMPT_VERSION_PREF_KEY = "latest_update_attempt_version"
        private const val LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY = "latest_update_attempt_time"
    }
}
