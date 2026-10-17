package org.fnives.tiktokdownloader.worker

import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.promise
import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.data.network.createHttpClient
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import org.fnives.tiktokdownloader.diagnostics.NoopDiagnostics
import kotlin.js.Promise

private val scope = CoroutineScope(Dispatchers.Default)

@OptIn(ExperimentalJsExport::class)
@JsExport
fun resolveVideo(url: String): Promise<ByteArray> = scope.promise {
    TikTokVideoResolver(
        delayBeforeRequest = 100,
        httpClient = createHttpClient(),
        diagnostics = NoopDiagnostics
    ).resolve(url).videoResponse.bodyAsBytes()
}
