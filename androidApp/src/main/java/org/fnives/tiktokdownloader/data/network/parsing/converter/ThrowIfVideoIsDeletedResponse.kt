package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException

class ThrowIfVideoIsDeletedResponse {

    private val potentialIssues = listOf(
        "\"statusMsg\":\"status_deleted",
        "\"statusMsg\":\"item doesn't exist",
        "statusMsg\":\"[^\"]*status_audit_not_pass"
    )

    private val redirectedToExplorePage = "\"seo.abtest\":{\"canonical\":\"https:\\u002F\\u002Fwww.tiktok.com\\u002Fexplore\""

    @Throws(VideoDeletedException::class)
    fun invoke(html: String) {
        potentialIssues.forEach {
            if (html.contains(it.toRegex())) {
                throw VideoDeletedException(html = html)
            }
            if (html.contains(redirectedToExplorePage)) {
                throw VideoDeletedException(html = html)
            }
        }
    }
}