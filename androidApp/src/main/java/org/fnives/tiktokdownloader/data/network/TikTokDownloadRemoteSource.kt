package org.fnives.tiktokdownloader.data.network

import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.javaio.toInputStream
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

class TikTokDownloadRemoteSource(
    private val videoResolver: TikTokVideoResolver,
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
                        contentType = response.contentType()?.let {
                            VideoInSavingIntoFile.ContentType(it.contentType, it.contentSubtype)
                        },
                        byteStream = response.bodyAsChannel().toInputStream(),
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
