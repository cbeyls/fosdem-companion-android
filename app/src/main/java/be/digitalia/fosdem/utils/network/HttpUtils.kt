package be.digitalia.fosdem.utils.network

import android.os.Build
import be.digitalia.fosdem.utils.BackgroundWorkScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.tls.HandshakeCertificates
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
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

    private val deferredClient = BackgroundWorkScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
        OkHttpClient.Builder()
                .enableTls12()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
    }

    val deferringCallFactory = Call.Factory { request ->
        runBlocking { deferredClient.await() }.newCall(request)
    }

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

        val client = deferredClient.await()

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

    private fun OkHttpClient.Builder.enableTls12(): OkHttpClient.Builder {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            val clientCertificates = HandshakeCertificates.Builder()
                    .addPlatformTrustedCertificates()
                    .build()
            sslSocketFactory(Tls12SocketFactory(clientCertificates.sslSocketFactory()), clientCertificates.trustManager())
        }
        return this
    }

    sealed class Response<out T> {
        object NotModified : Response<Nothing>()
        class Success<T>(val body: T, val raw: okhttp3.Response) : Response<T>()
    }
}