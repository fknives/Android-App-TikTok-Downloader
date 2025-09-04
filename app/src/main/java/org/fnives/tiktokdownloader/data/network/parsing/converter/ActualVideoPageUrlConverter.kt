package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.errortracking.ErrorTracer
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
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
            val validateResponse = responseBodyAsString
                .also(throwIfIsCaptchaResponse::invoke)
                .also(throwIfVideoIsDeletedResponse::invoke)
                .also(throwIfVideoIsPrivateResponse::invoke)
            val actualVideoPageUrl = if (validateResponse.contains("rel=\"canonical\" href=\"")) {
                validateResponse.split("rel=\"canonical\" href=\"")[1]
                    .split("\"")[0]
            } else if (validateResponse.contains("\"canonical\":\"")){
                validateResponse.split("\"canonical\":\"")[1]
                    .split("\"")[0]
                    .replace("\\u002F", "/")
            } else {
                throw ParsingException(message = "Can't find a way to get the proper URL from the video", html = responseBodyAsString)
            }

            ActualVideoPageUrl(actualVideoPageUrl, responseBodyAsString)
        } catch (throwable: Throwable) {
            val exceptionName = (throwable as? HtmlException)?.exceptionName ?: "Unknown Error"
            ErrorTracer.addError(
                html = responseBodyAsString,
                message = "$exceptionName in ActualVideoPageUrlConverter",
                throwable = throwable
            )
            ActualVideoPageUrl(null, responseBodyAsString)
        }

    }
}