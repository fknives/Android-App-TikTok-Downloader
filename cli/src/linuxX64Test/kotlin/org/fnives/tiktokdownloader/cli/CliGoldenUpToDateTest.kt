@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("TestFunctionName")

package org.fnives.tiktokdownloader.cli

import io.ktor.client.statement.bodyAsBytes
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.data.network.createHttpClient
import org.fnives.tiktokdownloader.diagnostics.NoopDiagnostics
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.getcwd
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * "Up-to-date" style check: it makes a real request and byte-compares the downloaded video against
 * the checked-in golden file(s) under `src/linuxX64Test/resources/video`.
 */
class CliGoldenUpToDateTest {

    @Test
    fun GIVEN_known_url_WHEN_downloading_THEN_it_matches_the_golden_video() {
        val download: ByteArray = runBlocking {
            TikTokVideoResolver(
                delayBeforeRequest = 1_000,
                httpClient = createHttpClient(),
                diagnostics = NoopDiagnostics
            ).resolve(URL).videoResponse.bodyAsBytes()
        }

        assertTrue(download.isNotEmpty(), "the download was empty")

        val mismatchOffsets = EXPECTED_FILE_PATHS.map { firstMismatch(download, findGolden(it)) }
        assertTrue(
            mismatchOffsets.any { it == -1 },
            "download (${download.size} bytes) matches none of ${EXPECTED_FILE_PATHS}; " +
                    "first differences at offsets $mismatchOffsets",
        )
    }

    /** Index of the first differing byte, or -1 when both arrays are equal. */
    private fun firstMismatch(a: ByteArray, b: ByteArray): Int {
        val shortest = if (a.size < b.size) a.size else b.size
        for (index in 0 until shortest) {
            if (a[index] != b[index]) return index
        }
        return if (a.size == b.size) -1 else shortest
    }

    /**
     * Kotlin/Native has no resource classpath, so the golden is read from disk: walk up from the
     * working directory to the module's `src/linuxX64Test/resources`.
     */
    private fun findGolden(relativePath: String): ByteArray {
        val relative = "cli/src/linuxX64Test/resources/$relativePath"
        var directory = currentDirectory()
        while (true) {
            val candidate = "$directory/$relative"
            if (fileExists(candidate)) return readFile(candidate)
            val parent = directory.substringBeforeLast('/', missingDelimiterValue = "")
            if (parent.isEmpty() || parent == directory) break
            directory = parent
        }

        val fallbacks = listOf(
            relativePath, // working directory is the processed test resources dir
            relative, // working directory is the repository root
            "src/linuxX64Test/resources/$relativePath", // working directory is the module dir
            "../cli/src/linuxX64Test/resources/$relativePath",
        )
        return fallbacks.firstOrNull(::fileExists)?.let(::readFile)
            ?: error("Could not locate $relative (tried walking up from $directory and $fallbacks)")
    }

    private fun currentDirectory(): String {
        val size = 4096
        val buffer = ByteArray(size)
        val result = buffer.usePinned { pinned -> getcwd(pinned.addressOf(0), size.convert()) }
        check(result != null) { "getcwd() failed" }
        return result.toKString()
    }

    private fun fileExists(path: String): Boolean {
        val file = fopen(path, "rb") ?: return false
        fclose(file)
        return true
    }

    private fun readFile(path: String): ByteArray {
        val file = fopen(path, "rb") ?: error("Could not open $path")
        try {
            val buffer = ByteArray(64 * 1024)
            var result = ByteArray(0)
            while (true) {
                val read = buffer.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1u.convert(), buffer.size.convert(), file)
                }
                if (read.toLong() <= 0L) break
                result += buffer.copyOf(read.toInt())
            }
            return result
        } finally {
            fclose(file)
        }
    }

    companion object {
        private const val URL = "https://vm.tiktok.com/ZN8SyMfnP/"
        private val EXPECTED_FILE_PATHS = listOf("video/expected_option_1.mp4")
    }
}
