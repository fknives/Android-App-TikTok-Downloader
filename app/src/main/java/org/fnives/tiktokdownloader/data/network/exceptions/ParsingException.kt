package org.fnives.tiktokdownloader.data.network.exceptions

import java.io.IOException

class ParsingException(message: String? = null, cause: Throwable? = null) : IOException(message, cause)