package be.digitalia.fosdem.inject

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DateTimeModule {
    private const val CONFERENCE_ZONE_OFFSET_HOURS = 1

    @Provides
    @Named("Conference")
    fun provideConferenceZoneId(): ZoneId {
        // No need to make it a singleton: ZoneOffset includes a cache
        return ZoneOffset.ofHours(CONFERENCE_ZONE_OFFSET_HOURS)
    }
}