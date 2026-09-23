package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.fnives.tiktokdownloader.data.model.VideoState

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

@Suppress("TestFunctionName")
@Timeout(value = 2)
class RemoveVideoFromQueueUseCaseTest {

    private lateinit var sut: RemoveVideoFromQueueUseCase
    private lateinit var mockVideoInPendingLocalSource: VideoInPendingLocalSource
    private lateinit var mockVideoDownloadedLocalSource: VideoDownloadedLocalSource

    @BeforeEach
    fun setup() {
        mockVideoInPendingLocalSource = mock()
        mockVideoDownloadedLocalSource = mock()
        sut = RemoveVideoFromQueueUseCase(mockVideoInPendingLocalSource, mockVideoDownloadedLocalSource)
    }

    @Test
    fun WHEN_no_action_THEN_no_delegation() {
        verifyNoInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_pending_video_WHEN_invoked_THEN_delegated() {
        val video = VideoState.InPending(VideoInPending("1", "url1"))
        sut.invoke(video)

        verify(mockVideoInPendingLocalSource).removeVideoFromQueue(video.videoInPending)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_downloaded_video_WHEN_invoked_THEN_delegated() {
        val video = VideoState.Downloaded(VideoDownloaded("1", "url1", "img1"))
        sut.invoke(video)

        verifyNoInteractions(mockVideoInPendingLocalSource)
        verify(mockVideoDownloadedLocalSource).removeVideo(video.videoDownloaded)
        verifyNoMoreInteractions(mockVideoDownloadedLocalSource)
    }

    @Test
    fun GIVEN_in_process_WHEN_invoked_THEN_no_delegation() {
        val video = VideoState.InProcess(VideoInProgress("1", "url1"))
        sut.invoke(video)

        verifyNoInteractions(mockVideoInPendingLocalSource)
        verifyNoInteractions(mockVideoDownloadedLocalSource)
    }
}