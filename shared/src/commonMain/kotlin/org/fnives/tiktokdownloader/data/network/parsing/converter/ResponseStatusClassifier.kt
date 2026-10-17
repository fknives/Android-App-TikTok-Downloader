package org.fnives.tiktokdownloader.data.network.parsing.converter

internal enum class ResponseStatus { DELETED, PRIVATE }

internal object ResponseStatusClassifier {

    private const val STATUS_MARKER = "statusMsg\":\""

    fun classify(html: String): ResponseStatus? {
        var from = 0
        var isPrivate = false
        while (true) {
            val markerIndex = html.indexOf(STATUS_MARKER, from)
            if (markerIndex < 0) return if (isPrivate) ResponseStatus.PRIVATE else null

            val valueStart = markerIndex + STATUS_MARKER.length
            val valueEnd = html.indexOf('"', valueStart)
            if (valueEnd < 0) return if (isPrivate) ResponseStatus.PRIVATE else null

            val value = html.substring(valueStart, valueEnd)
            if (value.startsWith("status_deleted") ||
                value.startsWith("item doesn't exist") ||
                value.contains("status_audit_not_pass")
            ) {
                return ResponseStatus.DELETED
            }
            if (value.startsWith("status_friend_see") || value.startsWith("author_secret")) {
                isPrivate = true
            }
            from = valueStart
        }
    }
}
