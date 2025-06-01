package be.digitalia.fosdem.api.network

import be.digitalia.fosdem.api.network.HttpClient.HttpResponse
import be.digitalia.fosdem.api.network.HttpClient.Response
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okio.BufferedSource
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * High-level coroutines-based HTTP client implementation based on OkHttp.
 *
 * @author Christophe Beyls
 */
class OkHttpClientImpl @Inject constructor(
    private val deferredCallFactory: @JvmSuppressWildcards Deferred<Call.Factory>
) : HttpClient {
    override suspend fun <T> get(url: String, lastModified: String?, httpResponseParser: (HttpResponse) -> T): Response<T> {
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
                            checkNotNull(body) { "Unexpected empty response body" }
                            val httpResponse = object : HttpResponse {
                                override val body: BufferedSource
                                    get() = body.source()
                                override val contentLength: Long
                                    get() = body.contentLength()
                                override val lastModified: String?
                                    get() = response.headers["Last-Modified"]
                            }
                            val parsedBody = httpResponseParser(httpResponse)
                            continuation.resume(Response.Success(parsedBody))
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
            continuation.invokeOnCancellation { call.cancel() }
        }
    }
}