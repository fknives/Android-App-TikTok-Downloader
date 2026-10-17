package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createHttpClient(): HttpClient = HttpClient(Js) { applyCommonConfig() }

/**
 * Cloudflare Workers reject a handful of headers (`origin`, `range`, `connection`, `cf-*`, ...).
 * Strip the ones this resolver would otherwise send so the same shared logic runs unchanged.
 */
internal actual fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String> =
    headers.filterKeys { key ->
        val lower = key.lowercase()
        lower != "origin" &&
            lower != "range" &&
            lower != "connection" &&
            !lower.startsWith("sec-fetch") &&
            !lower.startsWith("cf-")
    }
