package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import kotlin.jvm.Throws

class ActualVideoPageUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse
) : ParsingExceptionThrowingConverter<ActualVideoPageUrl>() {

    @Throws(IndexOutOfBoundsException::class, CaptchaRequiredException::class)
    override fun convertSafely(responseBody: ResponseBody): ActualVideoPageUrl? {
        val responseBodyAsString =responseBody.string()
        return try {
            val actualVideoPageUrl = responseBodyAsString
                .also(throwIfIsCaptchaResponse::invoke)
                .split("rel=\"canonical\" href=\"")[1]
                .split("\"")[0]

            ActualVideoPageUrl(actualVideoPageUrl, responseBodyAsString)
        } catch(_: Throwable) {
            ActualVideoPageUrl(null, responseBodyAsString)
        }

    }
}