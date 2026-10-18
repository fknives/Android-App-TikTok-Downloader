package org.fnives.tiktokdownloader.diagnostics

/**
 * Minimal logging/error-reporting.
 */
interface Diagnostics {
    fun log(message: String)
    fun error(html: String, message: String, throwable: Throwable?)
}

object NoopDiagnostics : Diagnostics {
    override fun log(message: String) {
    }

    override fun error(html: String, message: String, throwable: Throwable?) {
    }
}