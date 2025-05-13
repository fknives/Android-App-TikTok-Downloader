package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.VideoPrivateException

class ThrowIfVideoIsPrivateResponse {

    @Throws(VideoPrivateException::class)
    fun invoke(html: String) {
        if (html.contains("\"statusMsg\":\"status_friend_see")) {
            throw VideoPrivateException(html = html)
        } else if (html.contains("\"statusMsg\":\"author_secret")) {
            throw VideoPrivateException(html = html)
        }
    }
}