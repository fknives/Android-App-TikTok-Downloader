package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoPrivateException

/**
 * Throws the page-state exception when the response indicates that.
 */
class ThrowIfVideoIsNotPlayableResponse(
    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse = ThrowIfIsCaptchaResponse(),
) {

    private val redirectedToExplorePage =
        "\"seo.abtest\":{\"canonical\":\"https:\\u002F\\u002Fwww.tiktok.com\\u002Fexplore\""

    fun invoke(html: String) {
        throwIfIsCaptchaResponse.invoke(html)

        val status = ResponseStatusClassifier.classify(html)
        if (html.contains(redirectedToExplorePage) || status == ResponseStatus.DELETED) {
            throw VideoDeletedException(html = html)
        }
        if (status == ResponseStatus.PRIVATE) {
            throw VideoPrivateException(html = html)
        }
    }
}
