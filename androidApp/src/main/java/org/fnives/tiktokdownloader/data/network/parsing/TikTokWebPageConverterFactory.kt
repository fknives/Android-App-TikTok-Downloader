package org.fnives.tiktokdownloader.data.network.parsing

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.parsing.converter.ActualVideoPageUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfIsCaptchaResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsDeletedResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfVideoIsPrivateResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoResponseConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoResponse
import org.fnives.tiktokdownloader.errortracking.ErrorTracer
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class TikTokWebPageConverterFactory(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse,
    private val throwIfVideoIsDeletedResponse: ThrowIfVideoIsDeletedResponse,
    private val throwIfVideoIsPrivateResponse: ThrowIfVideoIsPrivateResponse,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
        when (type) {
            ActualVideoPageUrl::class.java -> ActualVideoPageUrlConverter(
                throwIfIsCaptchaResponse,
                throwIfVideoIsDeletedResponse,
                throwIfVideoIsPrivateResponse,
            )

            VideoFileUrl::class.java -> VideoFileUrlConverter(
                throwIfIsCaptchaResponse,
                throwIfVideoIsDeletedResponse,
                throwIfVideoIsPrivateResponse
            )

            VideoResponse::class.java -> VideoResponseConverter()
            else -> {
                ErrorTracer.addError(
                    "",
                    message = "Couldn't find proper Converter for $type with $annotations",
                    throwable = null
                )
                super.responseBodyConverter(type, annotations, retrofit)
            }
        }
}