@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.fnives.tiktokdownloader.cli

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import org.fnives.tiktokdownloader.data.network.CurlVideoHttpClient
import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import platform.posix.write
import kotlin.system.exitProcess

private const val STDOUT_FD = 1
private const val STDERR_FD = 2

/**
 * Minimal CLI:
 *
 *     tiktok-resolver <tiktok-share-url> > output.mp4
 *
 * Writes the raw video bytes to stdout, so it can be redirected straight into a file.
 */
fun main(args: Array<String>) {
    val url = args.firstOrNull()
    if (url.isNullOrBlank()) {
        writeToFd(STDERR_FD, "Usage: tiktok-resolver <tiktok-share-url> > output.mp4\n")
        exitProcess(1)
    }

    val resolver = TikTokVideoResolver(
        delayBeforeRequest = 1_000,
        httpClient = CurlVideoHttpClient(),
        diagnostics = StderrDiagnostics,
    )

    val bytes = try {
        runBlocking {
            val result = resolver.resolve(url)
            writeToFd(STDERR_FD, "resolved: ${result.videoFileUrl}\n")
            result.videoResponse.body.raw()
        }
    } catch (throwable: Throwable) {
        writeToFd(STDERR_FD, "error: ${throwable::class.simpleName}: ${throwable.message}\n")
        exitProcess(2)
    }

    writeToFd(STDOUT_FD, bytes)
}

/** Keeps the resolver's logging/error output on stderr so stdout stays a clean binary stream. */
private object StderrDiagnostics : Diagnostics {
    override fun log(message: String) {
        writeToFd(STDERR_FD, "TTDTag: $message\n")
    }

    override fun error(html: String, message: String, throwable: Throwable?) {
        writeToFd(STDERR_FD, "TTDTag: $message\n")
        throwable?.let { writeToFd(STDERR_FD, it.stackTraceToString() + "\n") }
    }
}

private fun writeToFd(fd: Int, text: String) = writeToFd(fd, text.encodeToByteArray())

private fun writeToFd(fd: Int, bytes: ByteArray) {
    if (bytes.isEmpty()) return
    bytes.usePinned { pinned ->
        var offset = 0
        while (offset < bytes.size) {
            val written = write(fd, pinned.addressOf(offset), (bytes.size - offset).convert())
            if (written <= 0) break
            offset += written.toInt()
        }
    }
}
