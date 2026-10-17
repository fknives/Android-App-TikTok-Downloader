package org.fnives.tiktokdownloader.data.network

/**
 * A response body, parameterised by the platform's raw payload [T].
 *
 * [text] is what the shared resolver reads; it is suspending because the bytes arrive async.
 * [raw] hands the payload to platform code untouched.
 */
interface HttpResponseBody<T> {
    suspend fun text(): String
    fun raw(): T
}

/**
 * The shared resolver talks to the network only through [VideoHttpClient].
 * [T] is the platform's raw body type, see [HttpResponseBody]
 */
class HttpResponseData<T>(
    val status: Int,
    val contentType: String?,
    val setCookies: List<String>,
    val body: HttpResponseBody<T>,
) {
    val isSuccess: Boolean get() = status in 200..299
}

/**
 * The seam the resolver uses to perform requests.
 * [T] is the platform's raw body type, see [HttpResponseBody]
 */
interface VideoHttpClient<T> {
    suspend fun get(url: String, headers: Map<String, String>): HttpResponseData<T>
}
