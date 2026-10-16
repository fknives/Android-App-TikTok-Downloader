package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl

actual fun createHttpClient(): HttpClient = HttpClient(Curl) { applyCommonConfig() }
