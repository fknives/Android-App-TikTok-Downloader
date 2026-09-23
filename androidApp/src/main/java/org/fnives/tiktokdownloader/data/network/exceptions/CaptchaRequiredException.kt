package org.fnives.tiktokdownloader.data.network.exceptions

class CaptchaRequiredException(
    message: String? = null, cause: Throwable? = null,
    override val html: String,
) : Throwable(message, cause), HtmlException {

    override val exceptionName: String get() = "CaptchaRequired"
}