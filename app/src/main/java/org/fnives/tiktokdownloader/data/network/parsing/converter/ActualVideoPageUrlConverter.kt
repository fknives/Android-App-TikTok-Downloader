package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import kotlin.jvm.Throws

class ActualVideoPageUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse
) : ParsingExceptionThrowingConverter<ActualVideoPageUrl>() {

    @Throws(IndexOutOfBoundsException::class, CaptchaRequiredException::class)
    override fun convertSafely(responseBody: ResponseBody): ActualVideoPageUrl? =
        responseBody.string()
            .also(throwIfIsCaptchaResponse::invoke)
            .split("rel=\"canonical\" href=\"")[1]
            .split("\"")[0]
            .let(::ActualVideoPageUrl)
}