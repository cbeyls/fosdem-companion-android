package be.digitalia.fosdem.inject

import android.os.Build
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.network.Tls12SocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val DEFAULT_CONNECT_TIMEOUT = 10L
    private const val DEFAULT_READ_TIMEOUT = 10L

    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .enableTls12()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    private fun OkHttpClient.Builder.enableTls12(): OkHttpClient.Builder {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            val clientCertificates = HandshakeCertificates.Builder()
                .addPlatformTrustedCertificates()
                .build()
            sslSocketFactory(
                Tls12SocketFactory(clientCertificates.sslSocketFactory()),
                clientCertificates.trustManager()
            )
        }
        return this
    }

    /**
     * Returns a Call.Factory lazily initialized on a background thread
     */
    @Provides
    @Singleton
    fun provideDeferredCallFactory(lazyClient: dagger.Lazy<OkHttpClient>): Deferred<Call.Factory> {
        return BackgroundWorkScope.async(Dispatchers.IO, CoroutineStart.LAZY) { lazyClient.get() }
    }
}