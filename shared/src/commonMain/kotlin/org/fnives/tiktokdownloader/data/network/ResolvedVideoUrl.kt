package org.fnives.tiktokdownloader.data.network

import io.ktor.client.statement.HttpResponse

/**
 * Outcome of resolving a TikTok share link.
 *
 * @param videoFileUrl the resolved playable video file URL.
 * @param videoResponse the already performed response for [videoFileUrl].
 */
data class ResolvedVideoUrl(
    val videoFileUrl: String,
    val videoResponse: HttpResponse,
)
