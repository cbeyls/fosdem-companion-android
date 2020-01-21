package be.digitalia.fosdem.utils.network

import android.os.Build
import be.digitalia.fosdem.utils.ByteCountSource
import be.digitalia.fosdem.utils.ByteCountSource.ByteCountListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Utility class to perform HTTP requests.
 *
 * @author Christophe Beyls
 */
object HttpUtils {

    private const val DEFAULT_CONNECT_TIMEOUT = 10L
    private const val DEFAULT_READ_TIMEOUT = 10L

    private val client = OkHttpClient.Builder()
            .enableTls12()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .build()

    @Throws(IOException::class)
    fun get(path: String): BufferedSource? {
        return when (val response = get(URL(path), null, null)) {
            is Response.NotModified -> null
            is Response.Success -> response.source
        }
    }

    /**
     * @param progressListener optional listener for the download progress in percents (0..100)
     */
    @Throws(IOException::class)
    fun get(path: String, lastModified: String? = null, progressListener: ((Int) -> Unit)? = null): Response {
        return get(URL(path), lastModified, progressListener)
    }

    /**
     * @param progressListener optional listener for the download progress in percents (0..100)
     */
    @Throws(IOException::class)
    fun get(url: URL, lastModified: String? = null, progressListener: ((Int) -> Unit)? = null): Response {
        val requestBuilder = Request.Builder()
        if (lastModified != null) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }

        val request = requestBuilder
                .url(url)
                .build()

        val okhttpResponse = client.newCall(request).execute()
        val body = okhttpResponse.body()
        if (!okhttpResponse.isSuccessful || body == null) {
            if (okhttpResponse.code() == HttpURLConnection.HTTP_NOT_MODIFIED && lastModified != null) {
                // Cached result is still valid; return an empty response
                return Response.NotModified
            }

            body?.close()
            throw IOException("Server returned response code: " + okhttpResponse.code())
        }

        val responseLastModified = okhttpResponse.header("Last-Modified")

        val length = body.contentLength()
        val source = if (progressListener != null && length != -1L) {
            // Broadcast the progression in percents, with a precision of 1/10 of the total file size
            val byteCountListener = object : ByteCountListener {
                override fun onNewCount(byteCount: Long) {
                    // Cap percent to 100
                    val percent = if (byteCount >= length) 100 else (byteCount * 100L / length).toInt()
                    progressListener(percent)
                }
            }
            ByteCountSource(body.source(), byteCountListener, length / 10L).buffer()
        } else {
            body.source()
        }

        return Response.Success(source, responseLastModified)
    }

    private fun OkHttpClient.Builder.enableTls12(): OkHttpClient.Builder {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                    "Unexpected default trust managers: " + Arrays.toString(trustManagers)
                }
                val trustManager = trustManagers[0] as X509TrustManager

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

                sslSocketFactory(Tls12SocketFactory(sslContext.socketFactory), trustManager)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    sealed class Response {
        object NotModified : Response()
        class Success(val source: BufferedSource, val lastModified: String? = null) : Response()
    }
}