package org.fnives.tiktokdownloader.worker

import org.w3c.fetch.Headers
import org.w3c.fetch.Request
import org.w3c.fetch.Response
import org.w3c.fetch.ResponseInit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

private const val CACHE_TTL_SECONDS = 60

private const val CACHE_KEY_PREFIX = "https://download-cache.invalid/"

// Worker-runtime globals, not part of the DOM-compat declarations.
private external val caches: CacheStorage

private external interface CacheStorage {
    val default: Cache
}

private external interface Cache {
    fun put(request: Request, response: Response): Promise<Unit>
    fun match(request: Request): Promise<Response?>
}

private external val crypto: Crypto

private external interface Crypto {
    fun randomUUID(): String
}

private external interface CachedDownload {
    val fileUrl: String
    val cookieHeader: String?
}

internal class ResolvedDownload(
    val fileUrl: String,
    val cookieHeader: String?,
)

/**
 * Stores the resolved file behind a new random id and returns that id.
 */
internal suspend fun storeDownload(fileUrl: String, cookieHeader: String?): String? {
    try {
        val id = crypto.randomUUID()
        val payload: dynamic = js("({})")
        payload.fileUrl = fileUrl
        payload.cookieHeader = cookieHeader

        caches.default.put(cacheKey(id), cacheResponse(JSON.stringify(payload))).await()
        return id
    } catch (_: Throwable) {
        return null
    }
}

/**
 * Loads a cached resolution.
 * Returns null on a miss, on expiry, or when the Cache API is unavailable.
 */
internal suspend fun loadDownload(id: String): ResolvedDownload? {
    try {
        val response = caches.default.match(cacheKey(id)).await() ?: return null
        val payload = JSON.parse(response.text().await()).unsafeCast<CachedDownload>()
        return ResolvedDownload(payload.fileUrl, payload.cookieHeader)
    } catch (_: Throwable) {
        return null
    }
}

private fun cacheKey(id: String): Request =
    Request("$CACHE_KEY_PREFIX$id")

private fun cacheResponse(body: String): Response {
    val headers = Headers()
    headers.set("content-type", "application/json")
    headers.set("cache-control", "max-age=$CACHE_TTL_SECONDS")
    return Response(body, ResponseInit(status = 200.toShort(), headers = headers))
}

private suspend fun <T> Promise<T>.await(): T =
    suspendCoroutine { continuation ->
        then(
            onFulfilled = { value -> continuation.resume(value) },
            onRejected = { error -> continuation.resumeWithException(error) },
        )
    }
