package org.fnives.tiktokdownloader.data.network.exceptions

interface HtmlException {
    val html: String
    val exceptionName: String
}