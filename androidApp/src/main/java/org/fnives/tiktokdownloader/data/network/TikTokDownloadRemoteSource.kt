package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import org.fnives.tiktokdownloader.errortracking.ErrorTracer
import java.io.ByteArrayInputStream

class TikTokDownloadRemoteSource(
    private val videoResolver: TikTokVideoResolver<ByteArray>,
    private val diagnostics: Diagnostics,
) {

    @Throws(ParsingException::class, NetworkException::class, CaptchaRequiredException::class)
    suspend fun getVideo(videoInPending: VideoInPending): VideoInSavingIntoFile =
        withContext(Dispatchers.IO) {
            wrapIntoProperException {
                val resolved = videoResolver.resolve(videoInPending.url)
                try {
                    val response = resolved.videoResponse
                    VideoInSavingIntoFile(
                        id = videoInPending.id,
                        url = videoInPending.url,
                        contentType = response.contentType.toVideoContentType(),
                        byteStream = ByteArrayInputStream(response.body.raw()),
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

/** Turns e.g. `video/mp4; charset=utf-8` into [VideoInSavingIntoFile.ContentType]. */
private fun String?.toVideoContentType(): VideoInSavingIntoFile.ContentType? {
    val mediaType = this?.substringBefore(';')?.trim() ?: return null
    val parts = mediaType.split('/')
    if (parts.size < 2) return null
    return VideoInSavingIntoFile.ContentType(parts[0], parts[1])
}
