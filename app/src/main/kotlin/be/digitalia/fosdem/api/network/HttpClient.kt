package be.digitalia.fosdem.api.network

import okio.BufferedSource

/**
 * High-level coroutines-based HTTP client abstraction.
 *
 * @author Christophe Beyls
 */
interface HttpClient {

    /**
     * Important: it's the caller responsibility to close the response body.
     *
     * @param httpResponseParser A block of code to optionally transform the Http response on a background thread.
     */
    suspend fun <T> get(url: String, httpResponseParser: (HttpResponse) -> T): Response.Success<T> {
        return when (val response = get(url, null, httpResponseParser)) {
            // Can only receive NotModified if lastModified argument is non-null
            is Response.NotModified -> throw IllegalStateException()
            is Response.Success -> response
        }
    }

    /**
     * Important: it's the caller responsibility to close the response body.
     *
     * @param lastModified header value matching a previous "Last-Modified" response header.
     */
    suspend fun get(url: String, lastModified: String?): Response<HttpResponse> = get(url, lastModified) { it }

    /**
     * Important: it's the caller responsibility to close the response body.
     *
     * @param lastModified header value matching a previous "Last-Modified" response header.
     * @param httpResponseParser A block of code to optionally transform the Http response on a background thread.
     */
    suspend fun <T> get(url: String, lastModified: String?, httpResponseParser: (HttpResponse) -> T): Response<T>

    interface HttpResponse {
        val body: BufferedSource

        /**
         * @return The content length or -1 if unknown.
         */
        val contentLength: Long

        /**
         * @return The value of the Last-Modified header or null if absent.
         */
        val lastModified: String?
    }

    sealed interface Response<out T> {
        data object NotModified : Response<Nothing>
        class Success<T>(val body: T) : Response<T>
    }
}