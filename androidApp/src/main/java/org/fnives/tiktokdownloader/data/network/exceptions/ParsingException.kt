package org.fnives.tiktokdownloader.data.network.exceptions

import java.io.IOException

class ParsingException(
    message: String? = null, cause: Throwable? = null,
    override val html: String,
) : IOException(message, cause), HtmlException {
    override val exceptionName: String get() = "Parsing"
}