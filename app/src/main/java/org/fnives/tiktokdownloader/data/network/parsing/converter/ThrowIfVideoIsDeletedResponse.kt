package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException

class ThrowIfVideoIsDeletedResponse {

    @Throws(VideoDeletedException::class)
    fun invoke(html: String) {
        if (html.contains("\"statusMsg\":\"status_deleted")) {
            throw VideoDeletedException(html = html)
        } else if (html.contains("\"statusMsg\":\"item doesn't exist")) {
            throw VideoDeletedException(html = html)
        }
    }
}