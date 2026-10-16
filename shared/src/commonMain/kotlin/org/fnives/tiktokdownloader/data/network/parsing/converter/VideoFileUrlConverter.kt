package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics

class VideoFileUrlConverter(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse,
    private val throwIfVideoIsDeletedResponse: ThrowIfVideoIsDeletedResponse,
    private val throwIfVideoIsPrivateResponse: ThrowIfVideoIsPrivateResponse,
    private val diagnostics: Diagnostics,
) : ParsingExceptionThrowingConverter<VideoFileUrl>() {

    override fun convertSafely(responseBody: String): VideoFileUrl {
        return doActionSafely { convert(responseBody) }
    }

    private fun convert(responseBody: String): VideoFileUrl {
        try {
            val html = responseBody.also(throwIfIsCaptchaResponse::invoke)
                .also(throwIfVideoIsDeletedResponse::invoke)
                .also(throwIfVideoIsPrivateResponse::invoke)
            val url =
                tryToParseDownloadLink(html).also { diagnostics.log("parsed download link = $it") }
                    ?: tryToParseVideoSrc(html).also { diagnostics.log("parsed video src = $it") }
                    ?: throw IllegalArgumentException("Couldn't parse url from HTML: $html")

            return VideoFileUrl(url)
        } catch (throwable: Throwable) {
            val exceptionName = (throwable as? HtmlException)?.exceptionName ?: "Unknown Error"
            diagnostics.error(
                html = responseBody,
                message = "$exceptionName in VideoFileUrlConverter",
                throwable = throwable
            )
            throw throwable
        }
    }

    companion object {

        private fun tryToParseDownloadLink(html: String): String? =
            if (html.contains("\"playAddr\"")) {
                html.split("\"playAddr\"")[1]
                    .dropWhile { it != '\"' }.drop(1)
                    .takeWhile { it != '\"' }
                    .urlCharacterReplacements()
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
                    .urlCharacterReplacements()
            } else {
                null
            }

        private val replacements = mapOf(
            "\\u002F" to "/",
            "\\u0026" to "&"
        )

        private fun String.urlCharacterReplacements(): String =
            replacements.entries.fold(this) { result, toReplaceEntry ->
                result.replace(toReplaceEntry.key, toReplaceEntry.value)
            }
    }
}
