package org.fnives.tiktokdownloader.diagnostics

actual val platformDiagnostics: Diagnostics = object : Diagnostics {
    override fun log(message: String) {
        System.err.println("TTDTag: $message")
    }

    override fun error(html: String, message: String, throwable: Throwable?) {
        System.err.println("TTDTag: $message")
        throwable?.printStackTrace()
    }
}
