package org.fnives.tiktokdownloader.data.network.parsing

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.parsing.converter.ActualVideoPageUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfIsCaptchaResponse
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoFileUrlConverter
import org.fnives.tiktokdownloader.data.network.parsing.converter.VideoResponseConverter
import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoResponse
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class TikTokWebPageConverterFactory(private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
        when (type) {
            ActualVideoPageUrl::class.java -> ActualVideoPageUrlConverter(throwIfIsCaptchaResponse)
            VideoFileUrl::class.java -> VideoFileUrlConverter(throwIfIsCaptchaResponse)
            VideoResponse::class.java -> VideoResponseConverter()
            else -> super.responseBodyConverter(type, annotations, retrofit)
        }
}