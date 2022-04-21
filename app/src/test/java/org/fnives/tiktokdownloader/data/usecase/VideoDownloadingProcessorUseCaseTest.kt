package org.fnives.tiktokdownloader.data.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.fnives.tiktokdownloader.data.local.CaptchaTimeoutLocalSource
import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource
import org.fnives.tiktokdownloader.data.local.exceptions.StorageException
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.TikTokDownloadRemoteSource
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.helper.advanceTimeBy
import org.fnives.tiktokdownloader.helper.advanceUntilIdle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.InputStream

@Suppress("TestFunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
//@Timeout(value = 2)
class VideoDownloadingProcessorUseCaseTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockVideoInProgressLocalSource: VideoInProgressLocalSource
    private lateinit var mockVideoInPendingLocalSource: VideoInPendingLocalSource
    private lateinit var mockVideoDownloadedLocalSource: VideoDownloadedLocalSource
    private lateinit var mockTikTokDownloadRemoteSource: TikTokDownloadRemoteSource
    private lateinit var mockCaptchaTimeoutLocalSource: CaptchaTimeoutLocalSource
    private lateinit var videoInProgressMutableFlow: MutableStateFlow<VideoInProgress?>
    private lateinit var videoInPendingMutableFlow: MutableStateFlow<List<VideoInPending>>
    private lateinit var videoDownloadedMutableFlow: MutableStateFlow<List<VideoDownloaded>>
    private lateinit var sut: VideoDownloadingProcessorUseCase

    @BeforeEach
    fun setup() {
        mockVideoInProgressLocalSource = mock()
        mockVideoInPendingLocalSource = mock()
        mockVideoDownloadedLocalSource = mock()
        mockTikTokDownloadRemoteSource = mock()
        mockCaptchaTimeoutLocalSource = mock()
        videoInProgressMutableFlow = MutableStateFlow(null)
        videoInPendingMutableFlow = MutableStateFlow(emptyList())
        videoDownloadedMutableFlow = MutableStateFlow(emptyList())
        whenever(mockVideoInProgressLocalSource.videoInProcessFlow).doReturn(videoInProgressMutableFlow)
        whenever(mockVideoInPendingLocalSource.pendingVideos).doReturn(videoInPendingMutableFlow)
        whenever(mockVideoDownloadedLocalSource.savedVideos).doReturn(videoDownloadedMutableFlow)
        testDispatcher = StandardTestDispatcher()
        sut = VideoDownloadingProcessorUseCase(
            videoInProgressLocalSource = mockVideoInProgressLocalSource,
            videoInPendingLocalSource = mockVideoInPendingLocalSource,
            videoDownloadedLocalSource = mockVideoDownloadedLocalSource,
            tikTokDownloadRemoteSource = mockTikTokDownloadRemoteSource,
            captchaTimeoutLocalSource = mockCaptchaTimeoutLocalSource,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun WHEN_no_method_invoked_THEN_no_interaction_with_dependencies() {
        verifyNoInteractions(mockVideoInProgressLocalSource)
        verifyNoInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
        verifyNoInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_not_observing_WHEN_fetching_THEN_nothing_happens() {
        sut.fetchVideoInState()

        verifyNoInteractions(mockVideoInProgressLocalSource)
        verifyNoInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
        verifyNoInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_empty_pendingVideos_WHEN_observing_THEN_error_is_emited() = runBlocking {
        videoInPendingMutableFlow.value = emptyList()
        val expected = ProcessState.Finished
        val expectedList = listOf(expected)

        val resultList = async(testDispatcher) { sut.processState.take(1).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
        verifyNoInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_network_error_WHEN_observing_THEN_error_is_emited() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then { throw NetworkException() }
        val expected = ProcessState.NetworkError
        val expectedList = listOf(ProcessState.Processing(videoInPending), expected)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verify(mockTikTokDownloadRemoteSource, times(1)).getVideo(videoInPending)
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verifyNoMoreInteractions(mockTikTokDownloadRemoteSource)
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_parsing_error_WHEN_observing_THEN_parsingError_is_emited() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then { throw ParsingException() }
        val expected = ProcessState.ParsingError
        val expectedList = listOf(ProcessState.Processing(videoInPending), expected)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_one_pending_video_AND_unexpected_error_WHEN_observing_THEN_unknown_error_is_emitted() = runTest(testDispatcher) {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then { throw Throwable() }
        val expected = ProcessState.UnknownError
        val expectedList = listOf(ProcessState.Processing(videoInPending), expected)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_one_pending_video_AND_network_errors_WHILE_observing_WHEN_fetching_THEN_it_retries() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        var specificException = true
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw if (specificException) NetworkException().also { specificException = false } else Throwable()
        }
        val inProgressItem = ProcessState.Processing(videoInPending)
        val expectedList = listOf(inProgressItem, ProcessState.NetworkError, inProgressItem, ProcessState.UnknownError)

        val resultList = async(testDispatcher) { sut.processState.take(4).toList() }
        testDispatcher.advanceUntilIdle()
        sut.fetchVideoInState()
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_one_pending_video_AND_parsing_errors_WHILE_observing_WHEN_fetching_THEN_it_retries() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        var specificException = true
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw if (specificException) ParsingException().also { specificException = false } else Throwable()
        }
        val inProgressItem = ProcessState.Processing(videoInPending)
        val expectedList = listOf(inProgressItem, ProcessState.ParsingError, inProgressItem, ProcessState.UnknownError)

        val resultList = async(testDispatcher) { sut.processState.take(4).toList() }
        testDispatcher.advanceUntilIdle()
        sut.fetchVideoInState()
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_one_pending_video_AND_unknown_errors_WHILE_observing_WHEN_fetching_THEN_it_retries() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        var specificException = true
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw if (specificException) Throwable().also { specificException = false } else NetworkException()
        }
        val inProgressItem = ProcessState.Processing(videoInPending)
        val expectedList = listOf(inProgressItem, ProcessState.UnknownError, inProgressItem, ProcessState.NetworkError)

        val resultList = async(testDispatcher) { sut.processState.take(4).toList() }
        testDispatcher.advanceUntilIdle()
        sut.fetchVideoInState()
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
    }

    // verify that fetching even while request is running doesn't matter, only after error is emitted
    @Test
    fun GIVEN_one_pending_video_AND_delaying_until_fetch_WHILE_observing_WHEN_fetching_THEN_emition_happens_only_once() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        var specificException = true
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw if (specificException) {
                sut.fetchVideoInState()
                specificException = false

                NetworkException()
            } else {
                Throwable()
            }
        }
        val inProgressItem = ProcessState.Processing(videoInPending)
        val expectedList = listOf(inProgressItem, ProcessState.NetworkError)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockTikTokDownloadRemoteSource, times(1)).getVideo(videoInPending)
        verifyNoMoreInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_failing_request_WHEN_observing_THEN_video_is_marked_processing_then_unprocessing() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw NetworkException()
        }
        val inProgressItem = ProcessState.Processing(videoInPending)
        val expectedList = listOf(inProgressItem, ProcessState.NetworkError)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInProgressLocalSource, times(1)).markVideoAsInProgress(videoInPending)
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_successful_request_AND_storage_error_WHEN_observing_THEN_video_is_saved_called_and_error_is_propogated() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        val videoInSavingIntoFile = VideoInSavingIntoFile("x", "u", VideoInSavingIntoFile.ContentType("a", "b"), FalseInputStream())
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).doReturn(videoInSavingIntoFile)
        whenever(mockVideoDownloadedLocalSource.saveVideo(anyOrNull())).then {
            throw StorageException()
        }
        val expected = ProcessState.StorageError
        val expectedList = listOf(ProcessState.Processing(videoInPending), expected)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoDownloadedLocalSource, times(1)).saveVideo(videoInSavingIntoFile)
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_successful_request_AND_unexpected_error_WHEN_observing_THEN_video_is_saved_called_and_error_is_propogated() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        val videoInSavingIntoFile = VideoInSavingIntoFile("x", "u", VideoInSavingIntoFile.ContentType("a", "b"), FalseInputStream())
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).doReturn(videoInSavingIntoFile)
        whenever(mockVideoDownloadedLocalSource.saveVideo(anyOrNull())).then {
            throw Throwable()
        }
        val expected = ProcessState.UnknownError
        val expectedList = listOf(ProcessState.Processing(videoInPending), expected)

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoDownloadedLocalSource, times(1)).saveVideo(videoInSavingIntoFile)
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_successful_request_AND_successful_file_save_WHEN_observing_THEN_pending_is_removed() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        val videoDownloaded = VideoDownloaded("zz", "yy", "xx")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        val videoInSavingIntoFile = VideoInSavingIntoFile("x", "u", VideoInSavingIntoFile.ContentType("a", "b"), FalseInputStream())
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).doReturn(videoInSavingIntoFile)
        whenever(mockVideoDownloadedLocalSource.saveVideo(anyOrNull())).doReturn(videoDownloaded)
        val expectedList = listOf(
            ProcessState.Processing(videoInPending),
            ProcessState.Processed(videoDownloaded)
        )

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInPendingLocalSource, times(1)).removeVideoFromQueue(videoInPending)
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_captcha_timeout_WHEN_observing_THEN_captcha_timeout_is_emited() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockCaptchaTimeoutLocalSource.isInCaptchaTimeout()).doReturn(true)
        val expectedList = listOf(
            ProcessState.Processing(videoInPending),
            ProcessState.CaptchaError
        )

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_successful_request_AND_successful_file_save_WHEN_observing_with_2_THEN_pending_is_removed_AND_only_once_executed() =
        runBlocking {
            val videoInPending = VideoInPending("alma", "banan")
            val videoDownloaded = VideoDownloaded("zz", "yy", "xx")
            videoInPendingMutableFlow.value = listOf(videoInPending)
            val videoInSavingIntoFile = VideoInSavingIntoFile("x", "u", VideoInSavingIntoFile.ContentType("a", "b"), FalseInputStream())
            whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).doReturn(videoInSavingIntoFile)
            whenever(mockVideoDownloadedLocalSource.saveVideo(anyOrNull())).doReturn(videoDownloaded)
            val expectedList = listOf(
                ProcessState.Processing(videoInPending),
                ProcessState.Processed(videoDownloaded)
            )

            val resultList1 = async(testDispatcher) { sut.processState.take(2).toList() }
            val resultList2 = async(testDispatcher) { sut.processState.take(2).toList() }
            testDispatcher.advanceUntilIdle()

            Assertions.assertEquals(expectedList, resultList1.await())
            Assertions.assertEquals(expectedList, resultList2.await())
            verify(mockVideoInPendingLocalSource, times(1)).removeVideoFromQueue(videoInPending)
            verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
            verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        }

    @Test
    fun GIVEN_one_pending_video_BUT_already_downloaded_WHEN_observing_THEN_processed_is_emitted_but_no_request_call() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        val videoDownloaded = VideoDownloaded("alma", "banan", "xx")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        videoDownloadedMutableFlow.value = listOf(videoDownloaded)
        val expectedList = listOf(
            ProcessState.Processing(videoInPending),
            ProcessState.Processed(videoDownloaded)
        )

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verify(mockVideoInPendingLocalSource, times(1)).removeVideoFromQueue(videoInPending)
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
        verifyNoInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_one_pending_video_BUT_already_downloaded_AND_captcha_timeout_WHEN_observing_THEN_processed_is_emitted_but_no_request_call() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        val videoDownloaded = VideoDownloaded("alma", "banan", "xx")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        videoDownloadedMutableFlow.value = listOf(videoDownloaded)
        whenever(mockCaptchaTimeoutLocalSource.isInCaptchaTimeout()).doReturn(true)
        val expectedList = listOf(
            ProcessState.Processing(videoInPending),
            ProcessState.Processed(videoDownloaded)
        )

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verify(mockVideoInPendingLocalSource, times(1)).removeVideoFromQueue(videoInPending)
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
        verifyNoInteractions(mockTikTokDownloadRemoteSource)
    }

    @Test
    fun GIVEN_one_pending_video_BUT_CaptchaTimeoutException_WHEN_observing_THEN_its_saved_and_captchaError_emitted() = runBlocking {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        videoDownloadedMutableFlow.value = listOf()
        val expectedList = listOf(
            ProcessState.Processing(videoInPending),
            ProcessState.CaptchaError
        )
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then {
            throw CaptchaRequiredException()
        }

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verify(mockVideoInProgressLocalSource, times(1)).markVideoAsInProgress(videoInPending)
        verify(mockVideoInProgressLocalSource, times(1)).removeVideoAsInProgress(videoInPending)
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verify(mockTikTokDownloadRemoteSource, times(1)).getVideo(anyOrNull())
        verify(mockCaptchaTimeoutLocalSource, times(1)).isInCaptchaTimeout()
        verify(mockCaptchaTimeoutLocalSource, times(1)).onCaptchaResponseReceived()
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
        verifyNoMoreInteractions(mockTikTokDownloadRemoteSource)
        verifyNoMoreInteractions(mockCaptchaTimeoutLocalSource)
    }

    @Test
    fun GIVEN_one_pending_video_AND_not_advancing_enough_WHILE_observing_WHEN_fetching_THEN_nothing_is_called() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then { throw NetworkException() }

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceTimeBy(199)

        verifyNoInteractions(mockTikTokDownloadRemoteSource)
        testDispatcher.advanceUntilIdle()
        resultList.cancelAndJoin()
    }

    @Test
    fun GIVEN_one_pending_video_AND_but_advancing_enough_WHILE_observing_WHEN_fetching_THEN_nothing_is_called() = runBlocking<Unit> {
        val videoInPending = VideoInPending("alma", "banan")
        videoInPendingMutableFlow.value = listOf(videoInPending)
        whenever(mockTikTokDownloadRemoteSource.getVideo(videoInPending)).then { throw NetworkException() }

        val resultList = async(testDispatcher) { sut.processState.take(2).toList() }
        testDispatcher.advanceTimeBy(201)

        verify(mockTikTokDownloadRemoteSource, times(1)).getVideo(videoInPending)
        verifyNoMoreInteractions(mockTikTokDownloadRemoteSource)
        testDispatcher.advanceUntilIdle()
        resultList.cancelAndJoin()
    }

    class FalseInputStream : InputStream() {
        override fun read(): Int = 0
    }
}