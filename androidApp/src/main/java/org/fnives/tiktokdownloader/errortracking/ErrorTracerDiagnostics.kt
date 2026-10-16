package org.fnives.tiktokdownloader.errortracking

import org.fnives.tiktokdownloader.Config
import org.fnives.tiktokdownloader.diagnostics.Diagnostics

object ErrorTracerDiagnostics : Diagnostics {

    private const val TAG = "TTDTag"

    override fun log(message: String) {
        if (Config.DEBUG) {
            System.err.println("$TAG: $message")
        } else {
            ErrorTracer.addError("", message = "$TAG: $message", throwable = null)
        }
    }

    override fun error(html: String, message: String, throwable: Throwable?) {
        ErrorTracer.addError(html = html, message = message, throwable = throwable)
    }
}
