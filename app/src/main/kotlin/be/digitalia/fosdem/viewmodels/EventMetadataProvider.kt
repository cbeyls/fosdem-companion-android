package be.digitalia.fosdem.viewmodels

import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.settings.TimeZoneMode
import be.digitalia.fosdem.settings.UserSettingsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventMetadataProvider @Inject constructor(
    private val deviceZoneIdFlow: @JvmSuppressWildcards Flow<ZoneId>,
    userSettingsProvider: UserSettingsProvider,
    private val api: FosdemApi,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val timeZoneOverride: Flow<ZoneId?> = userSettingsProvider.timeZoneMode.flatMapLatest { timeZoneMode ->
        when (timeZoneMode) {
            TimeZoneMode.DEFAULT -> flowOf(null)
            TimeZoneMode.DEVICE -> deviceZoneIdFlow
        }
    }

    val roomStatuses: Flow<Map<String, RoomStatus>>
        get() = api.roomStatuses
}
