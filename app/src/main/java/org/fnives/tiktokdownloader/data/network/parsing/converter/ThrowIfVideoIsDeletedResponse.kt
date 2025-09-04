package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException

class ThrowIfVideoIsDeletedResponse {

    private val potentialIssues = listOf(
        "\"statusMsg\":\"status_deleted",
        "\"statusMsg\":\"item doesn't exist",
        "statusMsg\":\"[^\"]*status_audit_not_pass"
    )

    @Throws(VideoDeletedException::class)
    fun invoke(html: String) {
        potentialIssues.forEach {
            if (html.contains(it.toRegex())) {
                throw VideoDeletedException(html = html)
            }
        }
    }
}