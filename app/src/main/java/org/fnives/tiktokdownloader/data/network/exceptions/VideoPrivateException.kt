package org.fnives.tiktokdownloader.data.network.exceptions

class VideoPrivateException(override val html: String) : Throwable(),
    HtmlException {
    override val exceptionName: String get() = "Video Private"
    }