package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.test.runTest
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.diagnostics.NoopDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TikTokVideoResolverTest {

    private val canonicalUrl = "https://www.tiktok.com/@a/video/1"
    private val expectedVideoUrl = "https://cdn.tiktok.com/video.mp4&x=1"
    private val videoResponseBody = "<html>video-bytes</html>"

    private val canonicalResponseHtml =
        """<html><head><link rel="canonical" href="$canonicalUrl"></head></html>"""

    private val playAddrResponseHtml =
        """<html><script>{"playAddr":"https:\u002F\u002Fcdn.tiktok.com\u002Fvideo.mp4\u0026x=1"}</script></html>"""

    private fun resolverWith(vararg responses: String): TikTokVideoResolver<ByteArray> {
        val queue = ArrayDeque(responses.toList())
        return TikTokVideoResolver(
            delayBeforeRequest = 0,
            httpClient = FakeVideoHttpClient { _, _ -> queue.removeFirst() },
            diagnostics = NoopDiagnostics
        )
    }

    @Test
    fun GIVEN_share_link_WHEN_resolving_THEN_video_url_and_response_are_returned() = runTest {
        val resolver = resolverWith(canonicalResponseHtml, playAddrResponseHtml, videoResponseBody)

        val result = resolver.resolve("https://vm.tiktok.com/abc/")

        assertEquals(expectedVideoUrl, result.videoFileUrl)
        assertEquals(videoResponseBody, result.videoResponse.body.text())
    }

    @Test
    fun GIVEN_no_canonical_WHEN_resolving_THEN_video_file_url_is_parsed_from_the_first_response() = runTest {
        val resolver = resolverWith(playAddrResponseHtml, videoResponseBody)

        val result = resolver.resolve("https://vm.tiktok.com/abc/")

        assertEquals(expectedVideoUrl, result.videoFileUrl)
    }

    @Test
    fun GIVEN_captcha_response_WHEN_resolving_THEN_CaptchaRequiredException_is_thrown() = runTest {
        val resolver = resolverWith("""<html><script src="captcha.js"></script></html>""")

        assertFailsWith<CaptchaRequiredException> {
            resolver.resolve("https://vm.tiktok.com/abc/")
        }
    }

    @Test
    fun GIVEN_deleted_response_WHEN_resolving_THEN_VideoDeletedException_is_thrown() = runTest {
        val resolver = resolverWith("""<html>"statusMsg":"status_deleted"</html>""")

        assertFailsWith<VideoDeletedException> {
            resolver.resolve("https://vm.tiktok.com/abc/")
        }
    }
}

private class FakeBody(private val bytes: ByteArray) : HttpResponseBody<ByteArray> {
    override suspend fun text(): String = bytes.decodeToString()
    override fun raw(): ByteArray = bytes
}

private class FakeVideoHttpClient(
    private val bodyFor: (url: String, headers: Map<String, String>) -> String,
) : VideoHttpClient<ByteArray> {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponseData<ByteArray> =
        HttpResponseData(
            status = 200,
            contentType = "text/html",
            setCookies = emptyList(),
            body = FakeBody(bodyFor(url, headers).encodeToByteArray()),
        )
}
