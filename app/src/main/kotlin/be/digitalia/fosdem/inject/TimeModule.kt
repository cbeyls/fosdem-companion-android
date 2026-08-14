package be.digitalia.fosdem.inject

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.receiveBroadcasts
import be.digitalia.fosdem.utils.ElapsedRealTimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideTimeSource(): TimeSource = ElapsedRealTimeSource

    @Provides
    fun provideClock(): Clock = Clock.System

    @Provides
    fun provideDeviceZoneIdFlow(@ApplicationContext context: Context): Flow<ZoneId> {
        return callbackFlow {
            context.receiveBroadcasts(
                filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
                flags = ContextCompat.RECEIVER_EXPORTED,
            ) {
                trySend(ZoneId.systemDefault())
            }
        }
            .conflate()
            .onStart { emit(ZoneId.systemDefault()) }
    }
}
