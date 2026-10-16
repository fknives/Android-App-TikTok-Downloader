package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.fnives.tiktokdownloader.data.local.exceptions.StorageException
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.save.video.SaveVideoFile
import org.fnives.tiktokdownloader.data.local.verify.exists.StoredVideosCacheInvalidation
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExists
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.helper.mock.InMemorySharedPreferencesManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.InputStream


@Suppress("TestFunctionName")
@Timeout(value = 2)
class VideoDownloadedLocalSourceTest {

    private lateinit var sut: VideoDownloadedLocalSource
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var mockSaveVideoFile: SaveVideoFile
    private lateinit var mockVerifyFileForUriExists: VerifyFileForUriExists
    private lateinit var mockCacheInvalidation: StoredVideosCacheInvalidation

    @BeforeEach
    fun setup() {
        sharedPreferencesManager = InMemorySharedPreferencesManager()
        mockSaveVideoFile = mock()
        mockVerifyFileForUriExists = mock()
        mockCacheInvalidation = mock()

        sut = VideoDownloadedLocalSource(
            saveVideoFile = mockSaveVideoFile,
            sharedPreferencesManagerImpl = sharedPreferencesManager,
            verifyFileForUriExists = mockVerifyFileForUriExists,
            cacheInvalidation = mockCacheInvalidation
        )
    }

    @Test
    fun GIVEN_observing_saved_videos_WHEN_initialized_THEN_emptylist_is_emitted() = runBlocking<Unit> {
        Assertions.assertEquals(emptyList<VideoDownloaded>(), sut.savedVideos.first())
        verifyNoInteractions(mockSaveVideoFile)
        verifyNoInteractions(mockVerifyFileForUriExists)
    }

    @Test
    fun GIVEN_observing_saved_videos_WAITING_for_2_items_WHEN_initialization_THEN_it_timesOut() {
        Assertions.assertThrows(TimeoutCancellationException::class.java) {
            runBlocking<Unit> {
                withTimeout(300) {
                    sut.savedVideos.take(2).collect { }
                }
            }
        }
    }

    @Test
    fun GIVEN_observing_saved_videos_AND_uri_still_exists_WHEN_video_is_saved_THEN_videoDownloaded_is_sent_out() =
        runBlocking<Unit> {
            val videoInSavingIntoFile = VideoInSavingIntoFile(
                id = "id",
                url = "alma",
                contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                byteStream = FalseInputStream()
            )
            val expectedDir = "TikTok_Downloader"
            val expectedFileName = videoInSavingIntoFile.id + ".b"
            whenever(mockSaveVideoFile.invoke(expectedDir, expectedFileName, videoInSavingIntoFile)).doReturn("almaUri")
            whenever(mockVerifyFileForUriExists.invoke(listOf("almaUri"))).doReturn(setOf("almaUri"))

            val actual = async(coroutineContext) {
                sut.savedVideos.take(2).toList()
            }
            yield()
            sut.saveVideo(videoInSavingIntoFile)

            Assertions.assertEquals(listOf(VideoDownloaded("id", "alma", "almaUri")), sut.savedVideos.first())
            Assertions.assertEquals(listOf(emptyList(), listOf(VideoDownloaded("id", "alma", "almaUri"))), actual.await())
        }

    @Test
    fun GIVEN_observing_saved_videos_AND_uri_doesnt_exists_WHEN_video_is_saved_THEN_it_is_emitted_first_and_then_pruned() =
        runBlocking<Unit> {
            val videoInSavingIntoFile = VideoInSavingIntoFile(
                id = "id",
                url = "alma",
                contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                byteStream = FalseInputStream()
            )
            val expectedDir = "TikTok_Downloader"
            val expectedFileName = videoInSavingIntoFile.id + ".b"
            whenever(mockSaveVideoFile.invoke(expectedDir, expectedFileName, videoInSavingIntoFile)).doReturn("almaUri")
            whenever(mockVerifyFileForUriExists.invoke(listOf("almaUri"))).doReturn(emptySet())

            val actual = async(coroutineContext) {
                sut.savedVideos.take(3).toList()
            }
            yield()
            sut.saveVideo(videoInSavingIntoFile)

            Assertions.assertEquals(
                listOf(
                    emptyList<VideoDownloaded>(),
                    listOf(VideoDownloaded("id", "alma", "almaUri")),
                    emptyList<VideoDownloaded>(),
                ),
                actual.await()
            )
            Assertions.assertEquals(emptySet<String>(), sharedPreferencesManager.downloadedVideos)
        }

    @Test
    fun GIVEN_a_stored_video_whose_uri_doesnt_exist_WHEN_observing_THEN_it_is_emitted_and_then_pruned() =
        runBlocking<Unit> {
            val videoInSavingIntoFile = VideoInSavingIntoFile(
                id = "id",
                url = "alma",
                contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                byteStream = FalseInputStream()
            )
            whenever(mockSaveVideoFile.invoke(anyOrNull(), anyOrNull(), anyOrNull())).doReturn("almaUri")
            whenever(mockVerifyFileForUriExists.invoke(anyOrNull())).doReturn(emptySet())
            sut.saveVideo(videoInSavingIntoFile)

            val emissions = sut.savedVideos.take(2).toList()

            Assertions.assertEquals(
                listOf(
                    listOf(VideoDownloaded("id", "alma", "almaUri")),
                    emptyList<VideoDownloaded>(),
                ),
                emissions
            )
            Assertions.assertEquals(emptySet<String>(), sharedPreferencesManager.downloadedVideos)
        }

    @Test
    fun GIVEN_stored_video_whose_file_is_gone_WHEN_refreshing_THEN_cache_is_invalidated_and_it_is_pruned() =
        runBlocking<Unit> {
            val videoInSavingIntoFile = VideoInSavingIntoFile(
                id = "id",
                url = "alma",
                contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                byteStream = FalseInputStream()
            )
            whenever(mockSaveVideoFile.invoke(anyOrNull(), anyOrNull(), anyOrNull())).doReturn("almaUri")
            sut.saveVideo(videoInSavingIntoFile)
            whenever(mockVerifyFileForUriExists.invoke(anyOrNull())).doReturn(emptySet())

            sut.refreshStoredVideos()

            verify(mockCacheInvalidation, times(1)).invalidateCache()
            Assertions.assertEquals(emptySet<String>(), sharedPreferencesManager.downloadedVideos)
        }

    @Test
    fun GIVEN_exception_from_savingUseCase_WHEN_saving_video_THEN_the_exception_is_wrapped_into_FileException() {
        Assertions.assertThrows(StorageException::class.java) {
            runBlocking<Unit> {
                val videoInSavingIntoFile = VideoInSavingIntoFile(
                    id = "id",
                    url = "alma",
                    contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                    byteStream = FalseInputStream()
                )
                val expectedDir = "TikTok_Downloader"
                val expectedFileName = videoInSavingIntoFile.id + ".b"
                whenever(mockSaveVideoFile.invoke(expectedDir, expectedFileName, videoInSavingIntoFile))
                    .then { throw Throwable() }

                sut.saveVideo(videoInSavingIntoFile)
            }
        }
    }

    @Test
    fun GIVEN_readingResponseException_from_savingUseCase_WHEN_saving_video_THEN_it_is_not_wrapped_into_StorageException() {
        val thrown = Assertions.assertThrows(Throwable::class.java) {
            runBlocking<Unit> {
                val videoInSavingIntoFile = VideoInSavingIntoFile(
                    id = "id",
                    url = "alma",
                    contentType = VideoInSavingIntoFile.ContentType("a", "b"),
                    byteStream = FalseInputStream()
                )
                val expectedDir = "TikTok_Downloader"
                val expectedFileName = videoInSavingIntoFile.id + ".b"
                whenever(mockSaveVideoFile.invoke(expectedDir, expectedFileName, videoInSavingIntoFile))
                    .then { throw NetworkException(html = "", cause = Throwable()) }

                sut.saveVideo(videoInSavingIntoFile)
            }
        }

        Assertions.assertTrue(thrown is NetworkException)
        Assertions.assertFalse(thrown is StorageException)
    }

    @Test
    fun GIVEN_observing_saved_videos_WHEN_saving_2_videos_THEN_then_both_of_them_are_emitted_in_reverse_order() =
        runBlocking<Unit> {
            val videoInSavingIntoFile1 = VideoInSavingIntoFile(
                id = "id1",
                url = "alma1",
                contentType = VideoInSavingIntoFile.ContentType("a1", "b1"),
                byteStream = FalseInputStream()
            )
            val videoInSavingIntoFile2 = VideoInSavingIntoFile(
                id = "id2",
                url = "alma2",
                contentType = VideoInSavingIntoFile.ContentType("a2", "b2"),
                byteStream = FalseInputStream()
            )
            whenever(mockVerifyFileForUriExists.invoke(anyOrNull())).then { (it.arguments[0] as List<String>).toSet() }
            whenever(mockSaveVideoFile.invoke(anyOrNull(), anyOrNull(), anyOrNull())).then {
                "uri: " + (it.arguments[1] as String)
            }
            val expectedModel1 = VideoDownloaded(id = "id1", url = "alma1", uri = "uri: id1.b1")
            val expectedModel2 = VideoDownloaded(id = "id2", url = "alma2", uri = "uri: id2.b2")
            val expected = listOf(emptyList(), listOf(expectedModel1), listOf(expectedModel2, expectedModel1))
            val actual = async(coroutineContext) { sut.savedVideos.take(3).toList() }

            yield()
            sut.saveVideo(videoInSavingIntoFile1)
            delay(100)
            yield()
            sut.saveVideo(videoInSavingIntoFile2)

            Assertions.assertEquals(expected, actual.await())
        }

    @Test
    fun GIVEN_two_videos_WHEN_deleting_THEN_observer_is_updated() = runBlocking {
        val videoInSavingIntoFile1 = VideoInSavingIntoFile(
            id = "id1",
            url = "alma1",
            contentType = VideoInSavingIntoFile.ContentType("a1", "b1"),
            byteStream = FalseInputStream()
        )
        val videoInSavingIntoFile2 = VideoInSavingIntoFile(
            id = "id2",
            url = "alma2",
            contentType = VideoInSavingIntoFile.ContentType("a2", "b2"),
            byteStream = FalseInputStream()
        )
        whenever(mockVerifyFileForUriExists.invoke(anyOrNull())).then { (it.arguments[0] as List<String>).toSet() }
        whenever(mockSaveVideoFile.invoke(anyOrNull(), anyOrNull(), anyOrNull())).then {
            "uri: " + (it.arguments[1] as String)
        }
        val expectedModel1 = VideoDownloaded(id = "id1", url = "alma1", uri = "uri: id1.b1")
        val expectedModel2 = VideoDownloaded(id = "id2", url = "alma2", uri = "uri: id2.b2")
        val expected = listOf(
            emptyList(),
            listOf(expectedModel1),
            listOf(expectedModel2, expectedModel1),
            listOf(expectedModel2),
            emptyList(),
        )
        val actual = async(coroutineContext) { sut.savedVideos.take(expected.size).toList() }

        yield()
        sut.saveVideo(videoInSavingIntoFile1)
        delay(100)
        yield()
        sut.saveVideo(videoInSavingIntoFile2)
        yield()

        sut.removeVideo(expectedModel1)
        yield()
        sut.removeVideo(expectedModel2)
        yield()

        Assertions.assertIterableEquals(expected, actual.await())
    }

    class FalseInputStream : InputStream() {
        override fun read(): Int = 0
    }
}