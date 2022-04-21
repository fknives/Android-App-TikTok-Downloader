package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.di.module.NetworkModule
import org.fnives.tiktokdownloader.helper.getResourceFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File

/**
 * This is not a usual unit test:
 * Since the website may change anytime without any notice, this test verifies with actual request going out.
 * However this makes the test shaky, because if the device has no proper connection it may fail.
 */

@Timeout(value = 2)
class TikTokDownloadRemoteSourceUpToDateTest {

    private lateinit var sut: TikTokDownloadRemoteSource

    @BeforeEach
    fun setup() {
        sut = NetworkModule(1).tikTokDownloadRemoteSource
    }

    @Disabled("Can trigger captcha, so only run it separately")
    @Test
    fun GIVEN_actualVideo_WHEN_downloading_THEN_the_file_matching_with_the_previously_loaded_video() {
        val parameter = VideoInPending("123", SUBJECT_VIDEO_URL)
        val actualFile = File(ACTUAL_FILE_PATH)
        actualFile.delete()
        actualFile.createNewFile()
        actualFile.deleteOnExit()
        val expectedFile = getResourceFile(EXPECTED_FILE_PATH)
        actualFile.writeText("")

        runBlocking { sut.getVideo(parameter).byteStream }.use { inputStream ->
            actualFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Assertions.assertTrue(FileUtils.contentEquals(expectedFile, actualFile), "The Downloaded file Is Not Matching the expected")
    }

    companion object {
        private const val ACTUAL_FILE_PATH = "actual.mp4"
        private const val EXPECTED_FILE_PATH = "video/expected.mp4"
        private const val SUBJECT_VIDEO_URL = "https://vm.tiktok.com/ZSQG7SMf/"
    }
}