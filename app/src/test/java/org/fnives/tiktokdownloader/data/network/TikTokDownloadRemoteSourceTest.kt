package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.di.module.NetworkModule
import org.fnives.tiktokdownloader.helper.readResourceFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@Timeout(value = 2)
class TikTokDownloadRemoteSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sut: TikTokDownloadRemoteSource

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer().apply {
            start(PORT)
        }
        sut = NetworkModule(1).tikTokDownloadRemoteSource
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun GIVEN_request_finished_with_cookies_WHEN_request_is_called_THEN_cookieStore_is_clear_and_request_doesnt_contain_cookies() {
        val cookieToSet = "that-is-a-cookie"
        val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(shortenedUrlResponseBody)
            .setHeader("Set-Cookie", cookieToSet)
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        try {
            runBlocking { sut.getVideo(VideoInPending("", TEST_URL)) }
        } catch (ignored: Throwable) {
        }
        // after cookie is saved call again
        try {
            runBlocking { sut.getVideo(VideoInPending("", TEST_URL)) }
        } catch (ignored: Throwable) {
        }

        val cookieSavingRequest = mockWebServer.takeRequest()
        val failingRequestAfterCookieSave = mockWebServer.takeRequest()
        val newFirstRequest = mockWebServer.takeRequest()

        Assertions.assertEquals(null, cookieSavingRequest.headers["Cookie"])
        Assertions.assertEquals(cookieToSet, failingRequestAfterCookieSave.headers["Cookie"])
        Assertions.assertEquals(null, newFirstRequest.headers["Cookie"])
    }

    @Test
    fun GIVEN_cookie_in_first_response_THEN_it_is_used_in_the_following_requests() {
        val expectedCookie = "that-is-a-cookie"
        val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
        val mainPageVariant1Response = readResourceFileMainPageVariant1Response()
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setHeader("Set-Cookie", expectedCookie).setBody(shortenedUrlResponseBody)
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mainPageVariant1Response))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("banan"))
        val videoInPending = VideoInPending("alma", TEST_URL)

        runBlocking { sut.getVideo(videoInPending) }

        val firstRequest = mockWebServer.takeRequest()
        val secondRequest = mockWebServer.takeRequest()
        val thirdRequest = mockWebServer.takeRequest()

        Assertions.assertEquals(null, firstRequest.headers["Cookie"])
        Assertions.assertEquals(expectedCookie, secondRequest.headers["Cookie"])
        Assertions.assertEquals(expectedCookie, thirdRequest.headers["Cookie"])
    }

    @Test
    fun GIVEN_cookie_with_extra_info_in_first_response_THEN_it_is_used_in_the_following_request_without_the_extra() {
        val cookieWithExtra = "that-is-a-cookie; and-some-extra-unnecessary; things"
        val expectedCookie = "that-is-a-cookie"
        val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setHeader("Set-Cookie", cookieWithExtra).setBody(shortenedUrlResponseBody)
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val videoInPending = VideoInPending("alma", TEST_URL)

        try {
            runBlocking { sut.getVideo(videoInPending) }
        } catch (ignore: Throwable) {
        }

        val firstRequest = mockWebServer.takeRequest()
        val secondRequest = mockWebServer.takeRequest()

        Assertions.assertEquals(null, firstRequest.headers["Cookie"])
        Assertions.assertEquals(expectedCookie, secondRequest.headers["Cookie"])
    }

    @Test
    fun GIVEN_proper_responses_as_variant1_THEN_parsed_properly() = runBlocking<Unit> {
        val expectedId = "alma"
        val expectedUrl = TEST_URL
        val expectedContentType = VideoInSavingIntoFile.ContentType("video", "mp4")
        val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
        val mainPageVariant1Response = readResourceFileMainPageVariant1Response()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mainPageVariant1Response))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "video/mp4").setBody("banan"))
        val videoInPending = VideoInPending("alma", TEST_URL)


        val response = sut.getVideo(videoInPending)
        Assertions.assertEquals(expectedId, response.id)
        Assertions.assertEquals(expectedUrl, response.url)
        Assertions.assertEquals(expectedContentType, response.contentType)
        Assertions.assertEquals("banan", response.byteStream.reader().readText())
    }

    @Test
    fun GIVEN_proper_responses_as_variant2_THEN_parsed_properly() = runBlocking<Unit> {
        val expectedId = "e-alma"
        val expectedUrl = TEST_URL
        val expectedContentType = VideoInSavingIntoFile.ContentType("citrom", "mp4")
        val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
        val mainPageVariant1Response = readResourceFileMainPageVariant2Response()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mainPageVariant1Response))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "citrom/mp4").setBody("a-banan"))
        val videoInPending = VideoInPending("e-alma", TEST_URL)


        val response = sut.getVideo(videoInPending)
        Assertions.assertEquals(expectedId, response.id)
        Assertions.assertEquals(expectedUrl, response.url)
        Assertions.assertEquals(expectedContentType, response.contentType)
        Assertions.assertEquals("a-banan", response.byteStream.reader().readText())
    }

    @Test
    fun GIVEN_incorrect_first_response_THEN_NetworkException_is_thrown() {
        Assertions.assertThrows(NetworkException::class.java) {
            runBlocking<Unit> {
                mockWebServer.enqueue(MockResponse().setResponseCode(500))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @Test
    fun GIVEN_proper_first_incorrect_second_response_THEN_NetworkException_is_thrown() {
        Assertions.assertThrows(NetworkException::class.java) {
            runBlocking<Unit> {
                val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
                mockWebServer.enqueue(MockResponse().setResponseCode(500))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @Test
    fun GIVEN_proper_first_and_second_but_incorrect_video_response_THEN_NetworkException_is_thrown() {
        Assertions.assertThrows(NetworkException::class.java) {
            runBlocking<Unit> {
                val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
                val mainPageVariant1Response = readResourceFileMainPageVariant2Response()
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mainPageVariant1Response))
                mockWebServer.enqueue(MockResponse().setResponseCode(500))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @Test
    fun GIVEN_not_expected_response_body_as_first_THEN_ParsingException_is_thrown() {
        Assertions.assertThrows(ParsingException::class.java) {
            runBlocking<Unit> {
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("this cannot be parsed"))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @Test
    fun GIVEN_proper_first_but_not_expected_response_body_as_second_THEN_ParsingException_is_thrown() {
        Assertions.assertThrows(ParsingException::class.java) {
            runBlocking<Unit> {
                val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("this cannot be parsed"))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @MethodSource("captchaResponses")
    @ParameterizedTest(name = "GIVEN_{0}_captcha_response_at_first_THEN_CaptchaRequiredException_is_thrown")
    fun GIVEN_captcha_response_at_first_THEN_CaptchaRequiredException_is_thrown(
        captchaResponseFileName: String,
        response: String
    ) {
        Assertions.assertThrows(CaptchaRequiredException::class.java) {
            runBlocking<Unit> {
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(response))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    @MethodSource("captchaResponses")
    @ParameterizedTest(name = "GIVEN_proper_response_first_THEN_{0}_captcha_response_THEN_CaptchaRequiredException_is_thrown")
    fun GIVEN_proper_response_first_THEN_captcha_response_THEN_CaptchaRequiredException_is_thrown(
        captchaResponseFileName: String,
        response: String
    ) {
        Assertions.assertThrows(CaptchaRequiredException::class.java) {
            runBlocking<Unit> {
                val shortenedUrlResponseBody = readResourceFileShortenedUrlResponse()
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(shortenedUrlResponseBody))
                mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(response))

                sut.getVideo(VideoInPending("", TEST_URL))
            }
        }
    }

    companion object {
        private const val SHORTENED_URL_RESPONSE = "response/shortened_url_response.html"
        private const val CAPTCHA_REQUIRED_RESPONSE_ONE = "response/captcha_required_one.html"
        private const val CAPTCHA_REQUIRED_RESPONSE_TWO = "response/captcha_required_two.html"
        private const val MAIN_PAGE_VARIANT_1_RESPONSE = "response/main_page_v1.html"
        private const val MAIN_PAGE_VARIANT_2_RESPONSE = "response/main_page_v1.html"
        private const val PORT = 8080
        private const val TEST_URL = "http://127.0.0.1:$PORT"

        private fun Any.readResourceFileShortenedUrlResponse() =
            readResourceFile(SHORTENED_URL_RESPONSE)
                .replace("https://www.tiktok.com/@ieclauuu/video/6887614455967010049", TEST_URL)

        private fun Any.readResourceFileMainPageVariant1Response() =
            readResourceFile(MAIN_PAGE_VARIANT_1_RESPONSE)
                .replace(
                    "https://v16-web.tiktok.com/video/tos/alisg/tos-alisg-pve-0037c001/9ddfc12f43b04f6596f9953c9a9ca072/?a=1988\\u0026br=1534\\u0026bt=767\\u0026cr=0\\u0026cs=0\\u0026cv=1\\u0026dr=0\\u0026ds=3\\u0026er=\\u0026expire=1603682739\\u0026l=20201025212533010189074225590A080D\\u0026lr=tiktok_m\\u0026mime_type=video_mp4\\u0026policy=2\\u0026qs=0\\u0026rc=amlxbmV1O291eDMzMzczM0ApZDRoZDQ3Nzw1N2U5Nzs3O2dicW1vL2AxZV5fLS1iMTRzczA2Y2NgYTQ2LmE1Y2E0My46Yw%3D%3D\\u0026signature=cce079fd02e4dde94c1c93cfdbd1d100\\u0026tk=tt_webid_v2\\u0026vl=\\u0026vr=",
                    TEST_URL
                )

        private fun Any.readResourceFileMainPageVariant2Response() =
            readResourceFile(MAIN_PAGE_VARIANT_2_RESPONSE)
                .replace(
                    "https://v16-web.tiktok.com/video/tos/alisg/tos-alisg-pve-0037c001/9ddfc12f43b04f6596f9953c9a9ca072/?a=1988\\u0026br=1534\\u0026bt=767\\u0026cr=0\\u0026cs=0\\u0026cv=1\\u0026dr=0\\u0026ds=3\\u0026er=\\u0026expire=1603682739\\u0026l=20201025212533010189074225590A080D\\u0026lr=tiktok_m\\u0026mime_type=video_mp4\\u0026policy=2\\u0026qs=0\\u0026rc=amlxbmV1O291eDMzMzczM0ApZDRoZDQ3Nzw1N2U5Nzs3O2dicW1vL2AxZV5fLS1iMTRzczA2Y2NgYTQ2LmE1Y2E0My46Yw%3D%3D\\u0026signature=cce079fd02e4dde94c1c93cfdbd1d100\\u0026tk=tt_webid_v2\\u0026vl=\\u0026vr=",
                    TEST_URL
                )
        private fun Any.readCaptchaOneResponse() =
            readResourceFile(CAPTCHA_REQUIRED_RESPONSE_ONE)
                .replace("https://www.tiktok.com/@ieclauuu/video/6887614455967010049", TEST_URL)

        private fun Any.readCaptchaTwoResponse() =
            readResourceFile(CAPTCHA_REQUIRED_RESPONSE_TWO)
                .replace("https://www.tiktok.com/@ieclauuu/video/6887614455967010049", TEST_URL)

        @JvmStatic
        private fun captchaResponses() = Stream.of(
            Arguments.of(CAPTCHA_REQUIRED_RESPONSE_ONE, readCaptchaOneResponse()),
            Arguments.of(CAPTCHA_REQUIRED_RESPONSE_TWO, readCaptchaTwoResponse())
        )
    }
}