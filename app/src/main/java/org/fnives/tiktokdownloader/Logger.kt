package org.fnives.tiktokdownloader

import com.pierfrancescosoffritti.androidyoutubeplayer.BuildConfig
import org.fnives.tiktokdownloader.errortracking.ErrorTracer

object Logger {

    private const val TAG = "TTDTag"

    fun logMessage(message: String) {
        if (BuildConfig.DEBUG) {
            System.err.println("$TAG: $message")
        } else {
            ErrorTracer.addError("", message = "$TAG: $message", throwable = null)
        }
    }
}
