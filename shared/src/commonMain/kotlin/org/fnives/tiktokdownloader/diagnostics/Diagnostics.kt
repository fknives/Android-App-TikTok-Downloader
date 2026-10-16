package org.fnives.tiktokdownloader.diagnostics

/**
 * Minimal logging/error-reporting.
 */
interface Diagnostics {
    fun log(message: String)
    fun error(html: String, message: String, throwable: Throwable?)
}

expect val platformDiagnostics: Diagnostics
