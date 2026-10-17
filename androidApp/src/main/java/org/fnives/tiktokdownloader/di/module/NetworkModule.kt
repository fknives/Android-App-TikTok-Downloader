package org.fnives.tiktokdownloader.di.module

import org.fnives.tiktokdownloader.data.network.KtorVideoHttpClient
import org.fnives.tiktokdownloader.data.network.TikTokDownloadRemoteSource
import org.fnives.tiktokdownloader.data.network.TikTokVideoResolver
import org.fnives.tiktokdownloader.data.network.VideoHttpClient
import org.fnives.tiktokdownloader.diagnostics.Diagnostics
import org.fnives.tiktokdownloader.errortracking.ErrorTracerDiagnostics

class NetworkModule(
    private val delayBeforeRequest: Long,
    private val diagnostics: Diagnostics = ErrorTracerDiagnostics,
) {

    private val httpClient: VideoHttpClient<ByteArray> by lazy { KtorVideoHttpClient() }

    val tikTokDownloadRemoteSource: TikTokDownloadRemoteSource by lazy {
        TikTokDownloadRemoteSource(
            videoResolver = TikTokVideoResolver(
                delayBeforeRequest = delayBeforeRequest,
                httpClient = httpClient,
                diagnostics = diagnostics,
            ),
            diagnostics = diagnostics,
        )
    }
}
