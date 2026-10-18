@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.fnives.tiktokdownloader.data.network

import org.w3c.fetch.FOLLOW
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestRedirect
import org.w3c.fetch.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

fun createFetchVideoHttpClient(): VideoHttpClient<Response> = FetchVideoHttpClient()

/**
 * The lazy JS body: it keeps the `Response` and reads it on demand
 * The underlying body can only be consumed once!
 */
class FetchBody internal constructor(private val response: Response) : HttpResponseBody<Response> {

    override suspend fun text(): String = response.text().await()

    override fun raw(): Response = response
}

private suspend fun <T> Promise<T>.await(): T =
    suspendCoroutine { cont ->
        then(
            onFulfilled = { value -> cont.resume(value) },
            onRejected = { error -> cont.resumeWithException(error) },
        )
    }

private class FetchVideoHttpClient : VideoHttpClient<Response> {

    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): HttpResponseData<Response> {
        val fetchHeaders = Headers().apply {
            headers.forEach { (name, value) -> set(name, value) }
        }
        val init = RequestInit(method = "GET", redirect = RequestRedirect.FOLLOW, headers = fetchHeaders)

        val response = fetch(url, init).await()
        return HttpResponseData(
            status = response.status.toInt(),
            contentType = response.headers.get("content-type"),
            setCookies = response.readSetCookies(),
            body = FetchBody(response),
        )
    }
}

/** The global `fetch` provided by Node / worker */
private external fun fetch(input: String, init: RequestInit): Promise<Response>

/** This exists in Node / Worker */
private external interface HeadersWithSetCookie {
    fun getSetCookie(): Array<String>
}

private fun Response.readSetCookies(): List<String> =
    headers.unsafeCast<HeadersWithSetCookie>().getSetCookie().toList()

/**
 * Cloudflare Workers reject a handful of headers (`origin`, `range`, `connection`, `cf-*`, ...).
 * Strip the ones this resolver would otherwise send so the same shared logic runs unchanged.
 */
internal actual fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String> =
    headers.filterKeys { key ->
        val lower = key.lowercase()
        lower != "origin" &&
                lower != "range" &&
                lower != "connection" &&
                !lower.startsWith("sec-fetch") &&
                !lower.startsWith("cf-")
    }
