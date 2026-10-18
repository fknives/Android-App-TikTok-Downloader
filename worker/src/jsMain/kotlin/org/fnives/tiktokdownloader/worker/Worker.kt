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

        raw.withHeaders {
            set("content-type", raw.headers.get("content-type") ?: "video/mp4")
            set("content-disposition", "attachment; filename=\"${fileNameFrom(url)}\"")
            set("cache-control", "no-store")
            set("x-video-url", resolved.videoFileUrl)
            resolved.cookies?.let { set("x-video-cookie", it) }
        }
    }

/**
 * Re-fetches an already-resolved video file.
 */
@JsExport
fun downloadVideo(fileUrl: String, cookieHeader: String?): Promise<Response> =
    promise {
        val response = httpClient.get(fileUrl, videoFileRequestHeaders(cookieHeader))
        if (!response.isSuccess) {
            throw IllegalStateException("GET $fileUrl returned HTTP ${response.status}")
        }

        val raw = response.body.raw()
        raw.withHeaders {
            set("content-type", raw.headers.get("content-type") ?: "video/mp4")
            set("content-disposition", "attachment; filename=\"${fileNameFrom(fileUrl)}\"")
            set("cache-control", "no-store")
        }
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
