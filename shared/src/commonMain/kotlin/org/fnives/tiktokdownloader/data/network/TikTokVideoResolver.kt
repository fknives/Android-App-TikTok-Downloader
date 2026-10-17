package org.fnives.tiktokdownloader.data.network

import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.parsing.converter.ActualVideoPageUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfIsCaptchaResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsDeletedResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsPrivateResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import kotlin.time.Duration.Companion.milliseconds

private const val COOKIE_HEADER = "Cookie"

/**
 * Shared, platform-agnostic TikTok video URL resolver.
 */
class TikTokVideoResolver<T>(
    private val delayBeforeRequest: Long,
    private val httpClient: VideoHttpClient<T>,
    private val diagnostics: Diagnostics,
) {

    private val throwIfIsCaptchaResponse = ThrowIfIsCaptchaResponse()
    private val throwIfVideoIsDeletedResponse = ThrowIfVideoIsDeletedResponse()
    private val throwIfVideoIsPrivateResponse = ThrowIfVideoIsPrivateResponse()

    private val actualVideoPageUrlConverter = ActualVideoPageUrlConverter(
        throwIfIsCaptchaResponse,
        throwIfVideoIsDeletedResponse,
        throwIfVideoIsPrivateResponse,
        diagnostics,
    )
    private val videoFileUrlConverter = VideoFileUrlConverter(
        throwIfIsCaptchaResponse,
        throwIfVideoIsDeletedResponse,
        throwIfVideoIsPrivateResponse,
        diagnostics,
    )

    private val pageHeaders: Map<String, String> = sanitizeRequestHeaders(pageRequestHeaders)
    private val videoHeaders: Map<String, String> = sanitizeRequestHeaders(videoRequestHeaders)

    private val cookies = LinkedHashMap<String, String>()

    suspend fun resolve(tiktokLink: String): ResolvedVideoUrl<T> =
        wrapIntoProperException {
            cookies.clear() // start each resolution with a clean session
            platformDelay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
            diagnostics.log("starting request")
            val firstResponse = getHtml(tiktokLink)
            val actualUrl = actualVideoPageUrlConverter.convertSafely(firstResponse)
            val videoUrl: VideoFileUrl
            if (actualUrl.url != null) {
                diagnostics.log("actualUrl found = ${actualUrl.url}")
                platformDelay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
                videoUrl = videoFileUrlConverter.convertSafely(getHtml(actualUrl.url))
            } else {
                diagnostics.log("actualUrl not found. Attempting to parse videoUrl")
                videoUrl = videoFileUrlConverter.convertSafely(actualUrl.fullResponse)
            }
            diagnostics.log("videoFileUrl found = ${videoUrl.videoFileUrl}")

            platformDelay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
            val videoResponse = getChecked(videoUrl.videoFileUrl, videoHeaders)

            ResolvedVideoUrl(videoUrl.videoFileUrl, videoResponse)
        }

    private suspend fun getHtml(url: String): String =
        getChecked(url, pageHeaders).body.text()

    private suspend fun getChecked(url: String, headers: Map<String, String>): HttpResponseData<T> {
        val response = get(url, headers)
        if (!response.isSuccess) {
            throw IllegalStateException("GET $url returned HTTP ${response.status}")
        }
        return response
    }

    private suspend fun get(url: String, headers: Map<String, String>): HttpResponseData<T> {
        val requestHeaders = buildMap {
            putAll(headers)
            cookieHeader()?.let { put(COOKIE_HEADER, it) }
        }
        val response = httpClient.get(url, requestHeaders)
        storeCookies(response)
        return response
    }

    /**
     * Accumulates cookies across the whole resolution (like a browser cookie jar).
     */
    private fun storeCookies(response: HttpResponseData<T>) {
        response.setCookies.forEach { raw ->
            val pair = raw.substringBefore(';').trim()
            if ('=' in pair) {
                cookies[pair.substringBefore('=').trim()] = pair
            }
        }
    }

    private fun cookieHeader(): String? =
        cookies.values.takeIf { it.isNotEmpty() }?.joinToString("; ")

    private suspend fun <R> wrapIntoProperException(request: suspend () -> R): R =
        try {
            request()
        } catch (throwable: Throwable) {
            if (throwable is HtmlException) {
                diagnostics.error(
                    html = throwable.html,
                    message = throwable.message ?: "-",
                    throwable = throwable
                )
                throw throwable
            }
            diagnostics.error(
                html = "-",
                message = throwable.message ?: "-",
                throwable = throwable
            )
            throw NetworkException(
                cause = throwable,
                html = "wrapIntoProperException"
            )
        }
}
