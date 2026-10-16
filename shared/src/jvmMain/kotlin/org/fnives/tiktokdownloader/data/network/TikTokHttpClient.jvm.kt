package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) { applyCommonConfig() }

internal actual fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String> = headers
