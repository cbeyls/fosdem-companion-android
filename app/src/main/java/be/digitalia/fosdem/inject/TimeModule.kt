package be.digitalia.fosdem.inject

import be.digitalia.fosdem.utils.ElapsedRealTimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideTimeSource(): TimeSource = ElapsedRealTimeSource

    @Provides
    fun provideClock(): Clock = Clock.System
}