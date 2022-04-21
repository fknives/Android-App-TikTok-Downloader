package org.fnives.tiktokdownloader

object Logger {

    private const val TAG = "TTDTag"

    fun logMessage(message: String) {
        if (BuildConfig.DEBUG) {
            System.err.println("TTDTag $message")
        }
    }
}
