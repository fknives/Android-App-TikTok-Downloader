package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.fnives.tiktokdownloader.Logger
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoPrivateException
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.data.network.session.CookieStore
import org.fnives.tiktokdownloader.errortracking.ErrorTracer

class TikTokDownloadRemoteSource(
    private val delayBeforeRequest: Long,
    private val service: TikTokRetrofitService,
    private val cookieStore: CookieStore,
    private val videoFileUrlConverter: VideoFileUrlConverter,
) {

    @Throws(ParsingException::class, NetworkException::class, CaptchaRequiredException::class)
    suspend fun getVideo(videoInPending: VideoInPending): VideoInSavingIntoFile =
        withContext(Dispatchers.IO) {
            cookieStore.clear()
            wrapIntoProperException {
                ErrorTracer.startErrorTransaction(videoInPending.url)
                delay(delayBeforeRequest) // added just so captcha trigger may not happen
                Logger.logMessage("starting request")
                val actualUrl = service.getContentActualUrlAndCookie(videoInPending.url)
                val videoUrl: VideoFileUrl
                if (actualUrl.url != null) {
                    Logger.logMessage("actualUrl found = ${actualUrl.url}")
                    delay(delayBeforeRequest) // added just so captcha trigger may not happen

                    videoUrl = service.getVideoUrl(actualUrl.url)
                } else {
                    Logger.logMessage("actualUrl not found. Attempting to parse videoUrl")

                    videoUrl = videoFileUrlConverter.convertSafely(actualUrl.fullResponse)
                }
                Logger.logMessage("videoFileUrl found = ${videoUrl.videoFileUrl}")
                delay(delayBeforeRequest) // added just so captcha trigger may not happen
                try {
                    val response = service.getVideo(videoUrl.videoFileUrl)
                    ErrorTracer.cancelErrorTransaction()

                    VideoInSavingIntoFile(
                        id = videoInPending.id,
                        url = videoInPending.url,
                        contentType = response.mediaType?.let {
                            VideoInSavingIntoFile.ContentType(
                                it.type,
                                it.subtype
                            )
                        },
                        byteStream = response.videoInputStream
                    )
                } catch (throwable: Throwable) {
                    val exceptionName =
                        (throwable as? HtmlException)?.exceptionName ?: "Unknown Error"
                    ErrorTracer.addError(
                        "video-stream",
                        "$exceptionName error while service.getVideo",
                        throwable = throwable
                    )
                    throw throwable
                }
            }
        }

    @Throws(ParsingException::class, NetworkException::class, VideoDeletedException::class)
    private suspend fun <T> wrapIntoProperException(request: suspend () -> T): T =
        try {
            request()
        } catch (throwable: Throwable) {
            if (throwable is HtmlException) {
                ErrorTracer.addError(throwable.html, message = throwable.message ?: "-", throwable = throwable)
                throw throwable
            }
            ErrorTracer.addError(html = "-", message = throwable.message ?: "-", throwable = throwable)
            throw NetworkException(
                cause = throwable,
                html = "wrapIntoProperException"
            )
        } finally {
            ErrorTracer.commitErrorTransaction()
        }
}