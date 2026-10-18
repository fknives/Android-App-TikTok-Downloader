package org.fnives.tiktokdownloader.data.network

/**
 * Outcome of resolving a TikTok share link.
 *
 * @param videoFileUrl the resolved playable video file URL.
 * @param videoResponse the already performed response for [videoFileUrl].
 * It carries the cookies and headers the platform needs, so callers can stream the bytes from it directly.
 */
data class ResolvedVideoUrl<T>(
    val videoFileUrl: String,
    val videoResponse: HttpResponseData<T>,
    val cookies: String?
)
