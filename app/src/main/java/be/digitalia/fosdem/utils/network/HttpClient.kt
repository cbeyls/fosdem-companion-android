package be.digitalia.fosdem.utils.network

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * High-level coroutines-based HTTP client.
 *
 * @author Christophe Beyls
 */
class HttpClient @Inject constructor(private val deferredCallFactory: @JvmSuppressWildcards Deferred<Call.Factory>) {

    suspend fun <T> get(url: String, bodyParser: (body: ResponseBody, headers: Headers) -> T): Response.Success<T> {
        return when (val response = get(url, null, bodyParser)) {
            // Can only receive NotModified if lastModified argument is non-null
            is Response.NotModified -> throw IllegalStateException()
            is Response.Success -> response
        }
    }

    /**
     * @param lastModified header value matching a previous "Last-Modified" response header.
     */
    suspend fun <T> get(url: String, lastModified: String?, bodyParser: (body: ResponseBody, headers: Headers) -> T): Response<T> {
        val requestBuilder = Request.Builder()
        if (lastModified != null) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }
        val request = requestBuilder
            .url(url)
            .build()

        val call = deferredCallFactory.await().newCall(request)
        return suspendCancellableCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    // This block is invoked on OkHttp's network thread
                    val body = response.body
                    if (!response.isSuccessful) {
                        body?.close()
                        if (lastModified != null && response.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            // Cached result is still valid; return an empty response
                            continuation.resume(Response.NotModified)
                        } else {
                            continuation.resumeWithException(IOException("Server returned response code: " + response.code))
                        }
                    } else {
                        try {
                            val parsedBody = checkNotNull(body).use { bodyParser(it, response.headers) }
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

    sealed class Response<out T> {
        data object NotModified : Response<Nothing>()
        class Success<T>(val body: T, val raw: okhttp3.Response) : Response<T>()
    }

    companion object {
        const val LAST_MODIFIED_HEADER_NAME = "Last-Modified"
    }
}