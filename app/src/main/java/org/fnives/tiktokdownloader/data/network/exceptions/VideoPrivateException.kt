package org.fnives.tiktokdownloader.data.network.exceptions

class VideoPrivateException(override val html: String) : Throwable(),
    HtmlException