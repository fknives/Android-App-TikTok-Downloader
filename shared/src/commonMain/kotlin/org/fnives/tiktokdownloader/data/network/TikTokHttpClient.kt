package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

expect fun createHttpClient(): HttpClient

internal fun HttpClientConfig<*>.applyCommonConfig() {
    expectSuccess = true
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
}

private const val USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36"

/** Headers for the HTML pages (share link and canonical page). */
internal val pageRequestHeaders: Map<String, String> = mapOf(
    "Origin" to "https://www.tiktok.com",
    "Referer" to "https://www.tiktok.com/",
    "Sec-Fetch-Dest" to "empty",
    "Sec-Fetch-Mode" to "cors",
    "Sec-Fetch-Site" to "cross-site",
    "User-Agent" to USER_AGENT,
)

/**
 * Headers for the actual video file request, mirroring what the browser sends when the <video> element loads the file.
 */
internal val videoRequestHeaders: Map<String, String> = mapOf(
    "Referer" to "https://www.tiktok.com/",
    "Sec-Fetch-Dest" to "video",
    "Sec-Fetch-Mode" to "no-cors",
    "Sec-Fetch-Site" to "cross-site",
    "Accept" to "*/*",
    "Accept-Encoding" to "identity;q=1, *;q=0",
    "Accept-Language" to "en-US,en;q=0.9,hu-HU;q=0.8,hu;q=0.7,ro;q=0.6",
    "Connection" to "keep-alive",
    "Range" to "bytes=0-",
    "User-Agent" to USER_AGENT,
)

internal expect fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String>
