package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.fnives.tiktokdownloader.Logger
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.data.network.session.CookieStore

class TikTokDownloadRemoteSource(
    private val delayBeforeRequest: Long,
    private val service: TikTokRetrofitService,
    private val cookieStore: CookieStore,
    private val videoFileUrlConverter: VideoFileUrlConverter,
) {

    @Throws(ParsingException::class, NetworkException::class, CaptchaRequiredException::class)
    suspend fun getVideo(videoInPending: VideoInPending): VideoInSavingIntoFile = withContext(Dispatchers.IO) {
        cookieStore.clear()
        wrapIntoProperException {
            delay(delayBeforeRequest) // added just so captcha trigger may not happen
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
            val response = service.getVideo(videoUrl.videoFileUrl)

            VideoInSavingIntoFile(
                id = videoInPending.id,
                url = videoInPending.url,
                contentType = response.mediaType?.let { VideoInSavingIntoFile.ContentType(it.type, it.subtype) },
                byteStream = response.videoInputStream
            )
        }
    }

    @Throws(ParsingException::class, NetworkException::class)
    private suspend fun <T> wrapIntoProperException(request: suspend () -> T): T =
        try {
            request()
        } catch (parsingException: ParsingException) {
            throw parsingException
        } catch (captchaRequiredException: CaptchaRequiredException) {
            throw captchaRequiredException
        } catch (throwable: Throwable) {
            throw NetworkException(cause = throwable)
        }
}