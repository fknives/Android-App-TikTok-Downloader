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
    promise {
        val resolved = newResolver().resolve(url)
        val raw = resolved.videoResponse.body.raw()
        val id = storeDownload(resolved.videoFileUrl, resolved.cookies)

        raw.withHeaders {
            set("content-type", raw.headers.get("content-type") ?: "video/mp4")
            set("content-disposition", "attachment; filename=\"${fileNameFrom(url)}\"")
            set("cache-control", "no-store")
            if (id != null) {
                set(HEADER_DOWNLOAD_ID, id)
            }
        }
    }

/**
 * Downloads a video natively for the second pass.
 *
 * On an [id] cache hit it re-fetches the already-resolved file.
 * On a cache miss (or when no id was handed out) it resolves [url] end to end.
 */
@JsExport
fun downloadVideo(id: String?, url: String?): Promise<Response> =
    promise {
        val cached = id?.let { loadDownload(it) }

        val raw = if (cached != null) {
            try {
                fetchCached(cached)
            } catch (error: Throwable) {
                if (url != null) {
                    resolveFresh(url)
                } else {
                    throw error
                }
            }
        } else if (url != null) {
            resolveFresh(url)
        } else {
            return@promise errorResponse(400, "Missing ?url= query parameter")
        }

        val nameSource = url ?: cached?.fileUrl
        val fileName = if (nameSource != null) fileNameFrom(nameSource) else DEFAULT_FILE_NAME

        raw.withHeaders {
            set("content-type", raw.headers.get("content-type") ?: "video/mp4")
            set("content-disposition", "attachment; filename=\"${fileName}\"")
            set("cache-control", "no-store")
        }
    }

private suspend fun fetchCached(cached: ResolvedDownload): Response {
    val response = httpClient.get(cached.fileUrl, videoFileRequestHeaders(cached.cookieHeader))
    if (!response.isSuccess) {
        throw IllegalStateException("GET ${cached.fileUrl} returned HTTP ${response.status}")
    }
    return response.body.raw()
}

private suspend fun resolveFresh(url: String): Response =
    newResolver().resolve(url).videoResponse.body.raw()

private fun errorResponse(status: Int, message: String): Response {
    val payload: dynamic = js("({})")
    payload.error = message

    val headers = Headers()
    headers.set("content-type", "application/json; charset=utf-8")
    headers.set("cache-control", "no-store")
    return Response(JSON.stringify(payload), ResponseInit(status = status.toShort(), headers = headers))
}

/**
 * Last non-empty path segment of [url], mirroring the frontend's naming. Falls back
 * to [DEFAULT_FILE_NAME] when there is nothing usable.
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

private inline fun Response.withHeaders(crossinline extra: Headers.() -> Unit): Response {
    val newHeaders = Headers()
    this.headers.get("content-length")?.let {
        newHeaders.set("content-length", it)
        newHeaders.set("x-content-length", it)
    }
    newHeaders.extra()
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
