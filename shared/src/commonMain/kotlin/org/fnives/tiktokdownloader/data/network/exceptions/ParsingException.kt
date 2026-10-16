package org.fnives.tiktokdownloader.data.network.exceptions

class ParsingException(
    message: String? = null, cause: Throwable? = null,
    override val html: String,
) : Throwable(message, cause), HtmlException {
    override val exceptionName: String get() = "Parsing"
}
