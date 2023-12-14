package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoResponse

class VideoResponseConverter : ParsingExceptionThrowingConverter<VideoResponse>() {

    override fun convertSafely(responseBody: ResponseBody): VideoResponse =
        VideoResponse(responseBody.contentType(), responseBody.byteStream())
}