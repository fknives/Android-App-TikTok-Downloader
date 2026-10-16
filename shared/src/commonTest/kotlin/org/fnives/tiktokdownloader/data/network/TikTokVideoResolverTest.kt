package org.fnives.tiktokdownloader.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TikTokVideoResolverTest {

    private val canonicalUrl = "https://www.tiktok.com/@a/video/1"
    private val expectedVideoUrl = "https://cdn.tiktok.com/video.mp4&x=1"
    private val videoResponseHtml = "<html>video-bytes</html>"

    private val canonicalResponseHtml =
        """<html><head><link rel="canonical" href="$canonicalUrl"></head></html>"""

    private val playAddrResponseHtml =
        """<html><script>{"playAddr":"https:\u002F\u002Fcdn.tiktok.com\u002Fvideo.mp4\u0026x=1"}</script></html>"""

    private fun resolverWith(vararg responses: String): TikTokVideoResolver {
        val queue = ArrayDeque(responses.toList())
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = queue.removeFirst(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            }
        }
        return TikTokVideoResolver(delayBeforeRequest = 0, httpClient = client)
    }

    @Test
    fun GIVEN_share_link_WHEN_resolving_THEN_canonical_page_video_file_url_and_video_response_are_returned() = runTest {
        val resolver = resolverWith(canonicalResponseHtml, playAddrResponseHtml, videoResponseHtml)

        val result = resolver.resolve("https://vm.tiktok.com/abc/")

        assertEquals(canonicalUrl, result.actualPageUrl)
        assertEquals(expectedVideoUrl, result.videoFileUrl)
        assertEquals(videoResponseHtml, result.videoResponse.bodyAsText())
    }

    @Test
    fun GIVEN_no_canonical_WHEN_resolving_THEN_video_file_url_is_parsed_from_the_first_response() = runTest {
        val resolver = resolverWith(playAddrResponseHtml, videoResponseHtml)

        val result = resolver.resolve("https://vm.tiktok.com/abc/")

        assertEquals(null, result.actualPageUrl)
        assertEquals(expectedVideoUrl, result.videoFileUrl)
        assertEquals(videoResponseHtml, result.videoResponse.bodyAsText())
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
