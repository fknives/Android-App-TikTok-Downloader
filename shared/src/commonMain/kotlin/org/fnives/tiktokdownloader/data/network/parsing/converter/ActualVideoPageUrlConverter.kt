package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics

private const val CANONICAL_LINK_PREFIX = "rel=\"canonical\" href=\""
private const val CANONICAL_JSON_PREFIX = "\"canonical\":\""

class ActualVideoPageUrlConverter(
    private val throwIfVideoIsNotPlayableResponse: ThrowIfVideoIsNotPlayableResponse,
    private val diagnostics: Diagnostics,
) : ParsingExceptionThrowingConverter<ActualVideoPageUrl>() {

    override fun convertSafely(responseBody: String): ActualVideoPageUrl {
        val responseBodyAsString = responseBody
        return try {
            val validateResponse = responseBodyAsString
                .also(throwIfVideoIsNotPlayableResponse::invoke)

            val actualVideoPageUrl = validateResponse.valueAfter(CANONICAL_LINK_PREFIX, '"')
                ?: validateResponse.valueAfter(CANONICAL_JSON_PREFIX, '"')?.replace("\\u002F", "/")
                ?: throw ParsingException(
                    message = "Can't find a way to get the proper URL from the video",
                    html = responseBodyAsString
                )

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
