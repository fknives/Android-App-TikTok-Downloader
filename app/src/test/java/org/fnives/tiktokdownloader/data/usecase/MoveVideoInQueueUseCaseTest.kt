package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

@Suppress("TestFunctionName")
@Timeout(value = 2)
class MoveVideoInQueueUseCaseTest {

    private lateinit var sut: MoveVideoInQueueUseCase
    private lateinit var mockVideoInPendingLocalSource: VideoInPendingLocalSource

    @BeforeEach
    fun setup() {
        mockVideoInPendingLocalSource = mock()
        sut = MoveVideoInQueueUseCase(mockVideoInPendingLocalSource)
    }

    @Test
    fun WHEN_no_action_THEN_no_delegation() {
        verifyNoInteractions(mockVideoInPendingLocalSource)
    }

    @Test
    fun GIVEN_video_and_position_change_WHEN_invoked_THEN_delegated() {
        val video = VideoInPending("1", "url1")
        sut.invoke(video, 100)

        verify(mockVideoInPendingLocalSource).moveBy(video, 100)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
    }
}