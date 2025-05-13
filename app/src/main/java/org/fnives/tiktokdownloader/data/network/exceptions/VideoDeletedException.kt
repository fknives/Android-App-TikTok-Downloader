package org.fnives.tiktokdownloader.data.network.exceptions

class VideoDeletedException(override val html: String) : Throwable(),
    HtmlException