package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoPrivateException
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl

class ActualVideoPageUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse,
    private val throwIfVideoIsDeletedResponse: ThrowIfVideoIsDeletedResponse,
    private val throwIfVideoIsPrivateResponse: ThrowIfVideoIsPrivateResponse
) : ParsingExceptionThrowingConverter<ActualVideoPageUrl>() {

    @Throws(
        IndexOutOfBoundsException::class, CaptchaRequiredException::class,
        VideoDeletedException::class,
        VideoPrivateException::class,
    )
    override fun convertSafely(responseBody: ResponseBody): ActualVideoPageUrl {
        val responseBodyAsString = responseBody.string()
        return try {
            val actualVideoPageUrl = responseBodyAsString
                .also(throwIfIsCaptchaResponse::invoke)
                .also(throwIfVideoIsDeletedResponse::invoke)
                .also(throwIfVideoIsPrivateResponse::invoke)
                .split("rel=\"canonical\" href=\"")[1]
                .split("\"")[0]

            ActualVideoPageUrl(actualVideoPageUrl, responseBodyAsString)
        } catch (_: Throwable) {
            ActualVideoPageUrl(null, responseBodyAsString)
        }

    }
}