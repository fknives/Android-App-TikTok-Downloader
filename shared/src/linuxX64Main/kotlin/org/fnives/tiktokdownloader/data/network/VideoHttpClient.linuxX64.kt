package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/** Eager body: Ktor reads the bytes up front, so [text] is just a decode. */
private class CurlBody(private val bytes: ByteArray) : HttpResponseBody<ByteArray> {
    override suspend fun text(): String = bytes.decodeToString()
    override fun raw(): ByteArray = bytes
}

class CurlVideoHttpClient : VideoHttpClient<ByteArray> {

    private val client = HttpClient(Curl) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponseData<ByteArray> {
        val response = client.get(url) {
            headers.forEach { (name, value) -> header(name, value) }
        }
        return HttpResponseData(
            status = response.status.value,
            contentType = response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" },
            setCookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty(),
            body = CurlBody(response.bodyAsBytes()),
        )
    }
}
