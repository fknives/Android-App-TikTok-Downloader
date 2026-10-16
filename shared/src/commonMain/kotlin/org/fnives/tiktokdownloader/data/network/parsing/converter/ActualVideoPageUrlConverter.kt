package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics

class ActualVideoPageUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse,
    private val throwIfVideoIsDeletedResponse: ThrowIfVideoIsDeletedResponse,
    private val throwIfVideoIsPrivateResponse: ThrowIfVideoIsPrivateResponse,
    private val diagnostics: Diagnostics,
) : ParsingExceptionThrowingConverter<ActualVideoPageUrl>() {

    override fun convertSafely(responseBody: String): ActualVideoPageUrl {
        val responseBodyAsString = responseBody
        return try {
            val validateResponse = responseBodyAsString
                .also(throwIfIsCaptchaResponse::invoke)
                .also(throwIfVideoIsDeletedResponse::invoke)
                .also(throwIfVideoIsPrivateResponse::invoke)
            val actualVideoPageUrl = if (validateResponse.contains("rel=\"canonical\" href=\"")) {
                validateResponse.split("rel=\"canonical\" href=\"")[1]
                    .split("\"")[0]
            } else if (validateResponse.contains("\"canonical\":\"")) {
                validateResponse.split("\"canonical\":\"")[1]
                    .split("\"")[0]
                    .replace("\\u002F", "/")
            } else {
                throw ParsingException(
                    message = "Can't find a way to get the proper URL from the video",
                    html = responseBodyAsString
                )
            }

            ActualVideoPageUrl(actualVideoPageUrl, responseBodyAsString)
        } catch (throwable: Throwable) {
            val exceptionName = (throwable as? HtmlException)?.exceptionName ?: "Unknown Error"
            diagnostics.error(
                html = responseBodyAsString,
                message = "$exceptionName in ActualVideoPageUrlConverter",
                throwable = throwable
            )
            ActualVideoPageUrl(null, responseBodyAsString)
        }
    }
}
