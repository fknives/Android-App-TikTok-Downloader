@file:OptIn(ExperimentalJsExport::class)

package org.fnives.tiktokdownloader.worker

import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.data.network.VideoHttpClient
import org.fnives.tiktokdownloader.data.network.createFetchVideoHttpClient
import org.fnives.tiktokdownloader.data.network.videoFileRequestHeaders
import org.fnives.tiktokdownloader.diagnostics.NoopDiagnostics
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.fetch.ResponseInit
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.js.Promise

private const val DEFAULT_FILE_NAME = "video.mp4"
private const val HEADER_DOWNLOAD_ID = "x-download-id"
private const val TIKTOK_HOST = "tiktok.com"
private const val TIKTOK_HOST_SUFFIX = ".tiktok.com"

private val httpClient: VideoHttpClient<Response> by lazy { createFetchVideoHttpClient() }

private fun newResolver() = TikTokVideoResolver(
    delayBeforeRequest = 0,
    httpClient = httpClient,
    diagnostics = NoopDiagnostics
)

/**
 * Entry point bundled into the Cloudflare Worker. Returns the video as a downloadable response.
 */
@JsExport
fun resolveVideo(url: String): Promise<Response> =
    promise { resolveVideoResponse(url, generateId = true) }

/**
 * Downloads a video natively for the second pass.
 *
 * On an [id] cache hit it re-fetches the already-resolved file. On a cache miss (or
 * when no id was handed out) it resolves [url] end to end without generating an id.
 */
@JsExport
fun downloadVideo(id: String?, url: String?): Promise<Response> =
    promise {
        val cached = id?.let { loadDownload(it) }

        if (cached != null) {
            val raw = try {
                fetchCached(cached)
            } catch (error: Throwable) {
                if (url == null) {
                    throw error
                }
                return@promise resolveVideoResponse(url, generateId = false)
            }

            val nameSource = url?.let { normalizeTikTokUrl(it) } ?: cached.fileUrl
            return@promise raw.asDownload(fileNameFrom(nameSource))
        }

        if (url == null) {
            return@promise errorResponse(400, "Missing or unsupported ?url= query parameter")
        }

        resolveVideoResponse(url, generateId = false)
    }


/**
 * Resolves [url] and wraps the stream as a downloadable response.
 */
private suspend fun resolveVideoResponse(url: String, generateId: Boolean): Response {
    val normalized = normalizeTikTokUrl(url)
        ?: return errorResponse(400, "Only TikTok links are supported.")

    val resolved = newResolver().resolve(normalized)
    val raw = resolved.videoResponse.body.raw()
    val id = if (generateId) storeDownload(resolved.videoFileUrl, resolved.cookies) else null

    return raw.asDownload(fileNameFrom(normalized), id)
}

private suspend fun fetchCached(cached: ResolvedDownload): Response {
    val response = httpClient.get(cached.fileUrl, videoFileRequestHeaders(cached.cookieHeader))
    if (!response.isSuccess) {
        throw IllegalStateException("GET ${cached.fileUrl} returned HTTP ${response.status}")
    }
    return response.body.raw()
}

private fun errorResponse(status: Int, message: String): Response {
    val payload: dynamic = js("({})")
    payload.error = message

    val headers = Headers()
    headers.set("content-type", "application/json; charset=utf-8")
    headers.set("cache-control", "no-store")
    return Response(JSON.stringify(payload), ResponseInit(status = status.toShort(), headers = headers))
}

/**
 * Accepts only TikTok links, with or without a scheme.
 * Returns the normalized URL (`https://` added when missing) or null when the host is not TikTok.
 */
private fun normalizeTikTokUrl(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val withScheme = if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }

    val afterScheme = withScheme.substringAfter("://", missingDelimiterValue = "")
    if (afterScheme.isEmpty()) {
        return null
    }

    val authority = afterScheme
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')

    if (authority.isEmpty() || '@' in authority || ':' in authority) {
        return null
    }

    val host = authority.lowercase()
    return if (host == TIKTOK_HOST || host.endsWith(TIKTOK_HOST_SUFFIX)) withScheme else null
}

/**
 * Last non-empty path segment of [url].
 */
private fun fileNameFrom(url: String): String {
    val segment = url
        .substringBefore('?')
        .substringBefore('#')
        .trimEnd('/')
        .substringAfterLast('/')
        .replace("\"", "")
    return if (segment.isBlank()) DEFAULT_FILE_NAME else "$segment.mp4"
}

private fun Response.asDownload(fileName: String, id: String? = null): Response {
    val contentType = headers.get("content-type") ?: "video/mp4"
    val newHeaders = Headers().apply {
        headers.get("content-length")?.let {
            set("content-length", it)
            set("x-content-length", it)
        }
        set("content-type", contentType)
        set("content-disposition", "attachment; filename=\"$fileName\"")
        set("cache-control", "no-store")
        if (id != null) {
            set(HEADER_DOWNLOAD_ID, id)
        }
    }
    return Response(body, ResponseInit(status = status, headers = newHeaders))
}

private fun <T> promise(block: suspend () -> T): Promise<T> =
    Promise { resolve, reject ->
        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                result.fold(onSuccess = resolve, onFailure = reject)
            }
        })
    }
