package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.diagnostics.Diagnostics

private const val PLAY_ADDR_PREFIX = "\"playAddr\""
private const val VIDEO_TAG = "<video"
private const val VIDEO_END_TAG = "</video>"

class VideoFileUrlConverter(
    private val throwIfVideoIsNotPlayableResponse: ThrowIfVideoIsNotPlayableResponse,
    private val diagnostics: Diagnostics,
) : ParsingExceptionThrowingConverter<VideoFileUrl>() {

    override fun convertSafely(responseBody: String): VideoFileUrl {
        return doActionSafely { convert(responseBody) }
    }

    /**
     * The video file URL of an *already validated* page, or `null` when the page has none.
     */
    fun parseOrNull(responseBody: String): VideoFileUrl? =
        (tryToParseDownloadLink(responseBody) ?: tryToParseVideoSrc(responseBody))
            ?.let(::VideoFileUrl)

    private fun convert(responseBody: String): VideoFileUrl {
        try {
            val html = responseBody.also(throwIfVideoIsNotPlayableResponse::invoke)
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

        /** `"playAddr":"…"` — [String.valueAfterQuoted] avoids copying the rest of the page. */
        private fun tryToParseDownloadLink(html: String): String? =
            html.valueAfterQuoted(PLAY_ADDR_PREFIX, '"')?.urlCharacterReplacements()

        /** `<video … src="…">` — index-based so only the URL itself is copied. */
        private fun tryToParseVideoSrc(html: String): String? {
            val videoIndex = html.indexOf(VIDEO_TAG)
            if (videoIndex < 0) return null
            val videoEnd = html.indexOf(VIDEO_END_TAG, videoIndex)
            if (videoEnd < 0) return null

            val srcIndex = html.indexOf("src", videoIndex)
            if (srcIndex !in videoIndex..videoEnd) return null
            val equalsIndex = html.indexOf('=', srcIndex)
            if (equalsIndex !in srcIndex..videoEnd) return null
            val openingQuote = html.indexOf('"', equalsIndex)
            if (openingQuote !in equalsIndex..videoEnd) return null

            val urlStart = openingQuote + 1
            val urlEnd = html.indexOf('"', urlStart)
            if (urlEnd !in urlStart..videoEnd) return null

            return html.substring(urlStart, urlEnd).urlCharacterReplacements()
        }

        private fun String.urlCharacterReplacements(): String =
            replace("\\u002F", "/").replace("\\u0026", "&")
    }
}
