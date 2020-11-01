package org.fnives.tiktokdownloader.data.network.parsing.converter

import okhttp3.ResponseBody
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import kotlin.jvm.Throws

class VideoFileUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse
) : ParsingExceptionThrowingConverter<VideoFileUrl>() {

    @Throws(IllegalArgumentException::class, IndexOutOfBoundsException::class, CaptchaRequiredException::class)
    override fun convertSafely(responseBody: ResponseBody): VideoFileUrl? {
        val html = responseBody.string().also(throwIfIsCaptchaResponse::invoke)
        val url = tryToParseDownloadLink(html)
            ?: tryToParseVideoSrc(html)
            ?: throw IllegalArgumentException("Couldn't parse url from HTML: $html")

        return VideoFileUrl(url)
    }

    companion object {

        private fun tryToParseDownloadLink(html: String): String? =
            if (html.contains("\"playAddr\"")) {
                html.split("\"playAddr\"")[1]
                    .dropWhile { it != '\"' }.drop(1)
                    .takeWhile { it != '\"' }
                    .replace("\\u0026", "&")
            } else {
                null
            }

        private fun tryToParseVideoSrc(html: String): String? =
            if (html.contains("<video")) {
                html.split("<video")[1]
                    .split("</video>")[0]
                    .split("src")[1]
                    .dropWhile { it != '=' }
                    .dropWhile { it != '\"' }.drop(1)
                    .takeWhile { it != '\"' }
                    .replace("\\u0026", "&")
            } else {
                null
            }

    }
}