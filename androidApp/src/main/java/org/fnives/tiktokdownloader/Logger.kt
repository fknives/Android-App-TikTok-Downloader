package org.fnives.tiktokdownloader

import org.fnives.tiktokdownloader.errortracking.ErrorTracer

object Logger {

    private const val TAG = "TTDTag"

    fun logMessage(message: String) {
        if (Config.DEBUG) {
            System.err.println("$TAG: $message")
        } else {
            ErrorTracer.addError("", message = "$TAG: $message", throwable = null)
        }
    }
}
