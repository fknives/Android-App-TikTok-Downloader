package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.parsing.converter.ActualVideoPageUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfIsCaptchaResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsDeletedResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsPrivateResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import org.fnives.tiktokdownloader.diagnostics.platformDiagnostics
import kotlin.time.Duration.Companion.milliseconds

class TikTokVideoResolver(
    private val delayBeforeRequest: Long,
    private val httpClient: HttpClient,
    private val diagnostics: Diagnostics = platformDiagnostics,
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

    suspend fun resolve(tiktokLink: String): ResolvedVideoUrl =
        wrapIntoProperException {
            cookies.clear() // start each resolution with a clean session
            delay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
            diagnostics.log("starting request")
            val firstResponse = getHtml(tiktokLink)
            val actualUrl = actualVideoPageUrlConverter.convertSafely(firstResponse)
            val videoUrl: VideoFileUrl
            if (actualUrl.url != null) {
                diagnostics.log("actualUrl found = ${actualUrl.url}")
                delay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
                videoUrl = videoFileUrlConverter.convertSafely(getHtml(actualUrl.url))
            } else {
                diagnostics.log("actualUrl not found. Attempting to parse videoUrl")
                videoUrl = videoFileUrlConverter.convertSafely(actualUrl.fullResponse)
            }
            diagnostics.log("videoFileUrl found = ${videoUrl.videoFileUrl}")

            delay(delayBeforeRequest.milliseconds) // added just so captcha trigger may not happen
            val videoResponse = get(videoUrl.videoFileUrl, videoHeaders)

            ResolvedVideoUrl(videoUrl.videoFileUrl, videoResponse)
        }

    private suspend fun getHtml(url: String): String = get(url, pageHeaders).bodyAsText()

    private suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
        val response = httpClient.get(url) {
            headers.forEach { (name, value) -> header(name, value) }
            cookieHeader()?.let { header(HttpHeaders.Cookie, it) }
        }
        storeCookies(response)
        return response
    }

    private fun storeCookies(response: HttpResponse) {
        response.headers.getAll(HttpHeaders.SetCookie)?.forEach { raw ->
            val cookie = raw.takeWhile { it != ';' }.trim()
            if ('=' in cookie) {
                cookies[cookie.takeWhile { it != '=' }.trim()] = cookie
            }
        }
    }

    private fun cookieHeader(): String? =
        cookies.values.takeIf { it.isNotEmpty() }?.joinToString("; ")

    private suspend fun <T> wrapIntoProperException(request: suspend () -> T): T =
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
