@file:OptIn(ExperimentalJsExport::class)

package org.fnives.tiktokdownloader.worker

import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.data.network.VideoHttpClient
import org.fnives.tiktokdownloader.data.network.createFetchVideoHttpClient
import org.fnives.tiktokdownloader.diagnostics.NoopDiagnostics
import org.w3c.fetch.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.js.Promise

private val httpClient: VideoHttpClient<Response> by lazy { createFetchVideoHttpClient() }

private fun newResolver() = TikTokVideoResolver(
    delayBeforeRequest = 0,
    httpClient = httpClient,
    diagnostics = NoopDiagnostics
)

/**
 * Entry point bundled into the Cloudflare Worker.
 */
@JsExport
fun resolveVideo(url: String): Promise<Response> =
    promise { newResolver().resolve(url).videoResponse.body.raw() }

private fun <T> promise(block: suspend () -> T): Promise<T> =
    Promise { resolve, reject ->
        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                result.fold(onSuccess = resolve, onFailure = reject)
            }
        })
    }
