package be.digitalia.fosdem.utils.network

import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import java.net.HttpURLConnection
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun <T> get(url: String, bodyParser: (body: ResponseBody, rawResponse: okhttp3.Response) -> T): Response.Success<T> {
        return when (val response = get(url, null, bodyParser)) {
            // Can only receive NotModified if lastModified argument is non-null
            is Response.NotModified -> throw IllegalStateException()
            is Response.Success -> response
        }
    }

    /**
     * @param lastModified header value matching a previous "Last-Modified" response header.
     */
    suspend fun <T> get(url: String, lastModified: String?, bodyParser: (body: ResponseBody, rawResponse: okhttp3.Response) -> T): Response<T> {
        val requestBuilder = Request.Builder()
        if (lastModified != null) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }
        val request = requestBuilder
                .url(url)
                .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    // This block is invoked on OkHttp's network thread
                    val body = response.body()
                    if (!response.isSuccessful || body == null) {
                        body?.close()
                        if (lastModified != null && response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            // Cached result is still valid; return an empty response
                            continuation.resume(Response.NotModified)
                        } else {
                            continuation.resumeWithException(IOException("Server returned response code: " + response.code()))
                        }
                    } else {
                        try {
                            val parsedBody = body.use { bodyParser(it, response) }
                            continuation.resume(Response.Success(parsedBody, response))
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
            continuation.invokeOnCancellation { call.cancel() }
        }
    }

    val okhttp3.Response.lastModified: String?
        get() = header("Last-Modified")

    fun OkHttpClient.Builder.enableTls12(): OkHttpClient.Builder {
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

    sealed class Response<out T> {
        object NotModified : Response<Nothing>()
        class Success<T>(val body: T, val raw: okhttp3.Response) : Response<T>()
    }
}