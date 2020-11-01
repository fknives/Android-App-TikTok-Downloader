package org.fnives.tiktokdownloader.data.usecase

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.fnives.tiktokdownloader.data.model.VideoState
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("TestFunctionName")
class StateOfVideosObservableUseCaseTest {

    private lateinit var testDispatcher: TestCoroutineDispatcher
    private lateinit var mockVideoInProgressLocalSource: VideoInProgressLocalSource
    private lateinit var mockVideoInPendingLocalSource: VideoInPendingLocalSource
    private lateinit var mockVideoDownloadedLocalSource: VideoDownloadedLocalSource
    private lateinit var videoInProgressMutableFlow: MutableStateFlow<VideoInProgress?>
    private lateinit var videoInPendingMutableFlow: MutableStateFlow<List<VideoInPending>>
    private lateinit var videoDownloadedMutableFlow: MutableStateFlow<List<VideoDownloaded>>
    private lateinit var sut: StateOfVideosObservableUseCase

    @BeforeEach
    fun setup() {
        mockVideoInProgressLocalSource = mock()
        mockVideoInPendingLocalSource = mock()
        mockVideoDownloadedLocalSource = mock()
        videoInProgressMutableFlow = MutableStateFlow(null)
        videoInPendingMutableFlow = MutableStateFlow(emptyList())
        videoDownloadedMutableFlow = MutableStateFlow(emptyList())
        whenever(mockVideoInProgressLocalSource.videoInProcessFlow).doReturn(videoInProgressMutableFlow)
        whenever(mockVideoInPendingLocalSource.pendingVideos).doReturn(videoInPendingMutableFlow)
        whenever(mockVideoDownloadedLocalSource.savedVideos).doReturn(videoDownloadedMutableFlow)
        testDispatcher = TestCoroutineDispatcher()
        sut = StateOfVideosObservableUseCase(
            videoInProgressLocalSource = mockVideoInProgressLocalSource,
            videoInPendingLocalSource = mockVideoInPendingLocalSource,
            videoDownloadedLocalSource = mockVideoDownloadedLocalSource,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun WHEN_no_invoke_is_called_THEN_no_dependency_is_called() {
        verifyZeroInteractions(mockVideoDownloadedLocalSource)
        verifyZeroInteractions(mockVideoInPendingLocalSource)
        verifyZeroInteractions(mockVideoInProgressLocalSource)
    }

    @Test
    fun GIVEN_no_inProgress_AND_empty_pending_AND_empty_saved_THEN_emptyList_is_emitted() = runBlocking(testDispatcher) {
        videoInProgressMutableFlow.value = null
        videoInPendingMutableFlow.value = emptyList()
        videoDownloadedMutableFlow.value = emptyList()

        val result = async(testDispatcher) { sut.invoke().take(1).toList() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(listOf(emptyList<VideoState>()), result.await())
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verify(mockVideoInProgressLocalSource, times(1)).videoInProcessFlow
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
    }

    @Test
    fun GIVEN_inProgress_AND_empty_pending_AND_empty_saved_THEN_inProgress_is_emitted() = runBlocking(testDispatcher) {
        val videoInProgress = VideoInProgress("alma", "url")
        val expected = listOf<VideoState>(VideoState.InProcess(videoInProgress))
        val expectedList = listOf(emptyList(), expected)

        val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
        testDispatcher.advanceUntilIdle()

        videoInProgressMutableFlow.value = videoInProgress
        videoInPendingMutableFlow.value = emptyList()
        videoDownloadedMutableFlow.value = emptyList()

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expected, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
        verify(mockVideoDownloadedLocalSource, times(1)).savedVideos
        verify(mockVideoInPendingLocalSource, times(1)).pendingVideos
        verify(mockVideoInProgressLocalSource, times(1)).videoInProcessFlow
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoMoreInteractions(mockVideoInProgressLocalSource)
    }

    @Test
    fun GIVEN_inProgress_AND_pendingWithSameId_AND_empty_saved_THEN_inProgress_is_emitted() = runBlocking(testDispatcher) {
        val videoInProgress = VideoInProgress("alma", "url")
        val videoInPending = VideoInPending(id = videoInProgress.id, url = videoInProgress.url)
        val expected = listOf<VideoState>(VideoState.InProcess(videoInProgress))
        val expectedList = listOf(emptyList(), expected)

        val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
        testDispatcher.advanceUntilIdle()

        videoInProgressMutableFlow.value = videoInProgress
        videoInPendingMutableFlow.value = listOf(videoInPending)
        videoDownloadedMutableFlow.value = emptyList()

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expected, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_in_pending_AND_nothing_inprogress_AND_empty_saved_THEN_inPending_is_emitted() = runBlocking(testDispatcher) {
        val videoInPending = VideoInPending(id = "alma", url = "url")
        val expected = listOf<VideoState>(VideoState.InPending(videoInPending))
        val expectedList = listOf(emptyList(), expected)

        val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
        testDispatcher.advanceUntilIdle()

        videoInProgressMutableFlow.value = null
        videoInPendingMutableFlow.value = listOf(videoInPending)
        videoDownloadedMutableFlow.value = emptyList()

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expected, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_inProgress_AND_pendingWithSameId_AND_savedWithSameId_THEN_inProgress_And_saved_is_emitted() =
        runBlocking(testDispatcher) {
            val videoInProgress = VideoInProgress("alma", "url")
            val videoInPending = VideoInPending(id = videoInProgress.id, url = videoInProgress.url)
            val videoDownloaded = VideoDownloaded(id = videoInProgress.id, url = videoInProgress.url, uri = "uri")
            val expected = listOf(VideoState.InProcess(videoInProgress), VideoState.Downloaded(videoDownloaded))
            val expectedList = listOf(emptyList(), expected)

            val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
            testDispatcher.advanceUntilIdle()

            videoInProgressMutableFlow.value = videoInProgress
            videoInPendingMutableFlow.value = listOf(videoInPending)
            videoDownloadedMutableFlow.value = listOf(videoDownloaded)

            val result = async(testDispatcher) { sut.invoke().first() }
            testDispatcher.advanceUntilIdle()

            Assertions.assertEquals(expected, result.await())
            Assertions.assertEquals(expectedList, resultList.await())
        }

    @Test
    fun GIVEN_new_item_faster_than_debounce_THEN_only_the_last_items_are_emitted() = runBlocking(testDispatcher) {
        val videoInProgress = VideoInProgress("alma", "url")
        val expected = listOf(VideoState.InProcess(videoInProgress))
        val expectedList = listOf(expected)

        videoInProgressMutableFlow.value = null
        videoInPendingMutableFlow.value = emptyList()
        videoDownloadedMutableFlow.value = emptyList()
        val resultList = async(testDispatcher) { sut.invoke().take(1).toList() }
        testDispatcher.advanceTimeBy(199)

        videoInProgressMutableFlow.value = videoInProgress

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expected, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_new_item_slower_than_debounce_THEN_both_list_is_emitted() = runBlocking(testDispatcher) {
        val videoInProgress = VideoInProgress("alma", "url")
        val expected = listOf(VideoState.InProcess(videoInProgress))
        val expectedList = listOf(emptyList(), expected)

        videoInProgressMutableFlow.value = null
        videoInPendingMutableFlow.value = emptyList()
        videoDownloadedMutableFlow.value = emptyList()
        val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
        testDispatcher.advanceTimeBy(200)

        videoInProgressMutableFlow.value = videoInProgress

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expected, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
    }

    @Test
    fun GIVEN_processing_LATER_pendingWithSameId_THEN_no_new_emition_happens() = runBlocking(testDispatcher) {
        val videoInProgress = VideoInProgress("alma", "url")
        val videoInPendingSameAsProgress = VideoInPending(id = videoInProgress.id, url = videoInProgress.url)
        val videoInPendingOther = VideoInPending(id = "alma2", url = "url2")
        val expectedStart = listOf(VideoState.InProcess(videoInProgress))
        val expectedEnd = listOf(VideoState.InProcess(videoInProgress), VideoState.InPending(videoInPendingOther))
        val expectedList = listOf(expectedStart, expectedEnd)

        videoInProgressMutableFlow.value = videoInProgress
        videoInPendingMutableFlow.value = emptyList()
        videoDownloadedMutableFlow.value = emptyList()
        val resultList = async(testDispatcher) { sut.invoke().take(2).toList() }
        testDispatcher.advanceUntilIdle()

        videoInPendingMutableFlow.value = listOf(videoInPendingSameAsProgress)
        testDispatcher.advanceUntilIdle()
        videoInPendingMutableFlow.value = listOf(videoInPendingOther)

        val result = async(testDispatcher) { sut.invoke().first() }
        testDispatcher.advanceUntilIdle()

        Assertions.assertEquals(expectedEnd, result.await())
        Assertions.assertEquals(expectedList, resultList.await())
    }
}