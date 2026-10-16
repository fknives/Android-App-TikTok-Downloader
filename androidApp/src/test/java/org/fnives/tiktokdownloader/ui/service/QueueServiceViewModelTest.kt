package org.fnives.tiktokdownloader.ui.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.HasPendingVideosUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.helper.junit.rule.MainDispatcherExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@Suppress("TestFunctionName", "UnusedFlow")
@ExtendWith(MainDispatcherExtension::class)
class QueueServiceViewModelTest {

    private lateinit var videoDownloadingProcessorFlow: MutableSharedFlow<ProcessState>
    private lateinit var hasPendingVideosFlow: MutableStateFlow<Boolean>
    private lateinit var mockVideoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
    private lateinit var mockHasPendingVideosUseCase: HasPendingVideosUseCase
    private lateinit var mockAddVideoToQueueUseCase: AddVideoToQueueUseCase
    private lateinit var sut: QueueServiceViewModel

    @BeforeEach
    fun setup() {
        videoDownloadingProcessorFlow = MutableSharedFlow(replay = 1)
        hasPendingVideosFlow = MutableStateFlow(true)
        mockVideoDownloadingProcessorUseCase = mock()
        whenever(mockVideoDownloadingProcessorUseCase.processEvents).doReturn(
            videoDownloadingProcessorFlow
        )
        mockHasPendingVideosUseCase = mock()
        whenever(mockHasPendingVideosUseCase.invoke()).doReturn(hasPendingVideosFlow)
        mockAddVideoToQueueUseCase = mock()

        sut = QueueServiceViewModel(
            mockAddVideoToQueueUseCase,
            mockHasPendingVideosUseCase,
            mockVideoDownloadingProcessorUseCase
        )
    }

    @Test
    fun GIVEN_initialized_WHEN_valid_url_fetched_THEN_addVideoToQueue_is_called_and_processor_kicked() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.fetch("url.com")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processEvents
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_unknown_url_fetched_THEN_it_is_still_forwarded_and_processor_kicked() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(false)
        sut.fetch("url.com")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
    }

    @Test
    fun GIVEN_initialized_WHEN_more_urls_fetched_THEN_notification_collection_is_started_only_once() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.fetch("url.com")
        sut.fetch("other.org")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verify(mockAddVideoToQueueUseCase, times(1)).invoke("other.org")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processEvents
        verify(mockVideoDownloadingProcessorUseCase, times(2)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @ParameterizedTest(name = "GIVEN_pending_videos_WHEN_{0}_sent_THEN_notificationState_is_{1}")
    @MethodSource("processEventToNotificationState")
    fun GIVEN_pending_videos_WHEN_SpecificProcessEvent_sent_THEN_notificationState_is_correct(
        processEvent: ProcessState,
        expected: NotificationState
    ) {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.fetch("")
        videoDownloadingProcessorFlow.tryEmit(processEvent)

        Assertions.assertEquals(expected, sut.notificationState.value)
    }

    @Test
    fun GIVEN_no_pending_videos_WHEN_fetched_THEN_notificationState_is_Finish() {
        hasPendingVideosFlow.value = false

        sut.fetch(null)

        Assertions.assertEquals(NotificationState.Finish, sut.notificationState.value)
    }

    @Test
    fun GIVEN_onCleared_WHEN_item_is_emited_THEN_it_is_no_longer_observed_and_cannot_be() {
        sut.onClear()

        sut.fetch("alma")
        videoDownloadingProcessorFlow.tryEmit(ProcessState.UnknownError)

        Assertions.assertEquals(null, sut.notificationState.value)
    }

    companion object {
        @JvmStatic
        private fun processEventToNotificationState() = Stream.of(
            Arguments.of(
                ProcessState.Processing(VideoInPending("", "this-is-url")),
                NotificationState.Processing("this-is-url")
            ),
            Arguments.of(
                ProcessState.Processed(VideoDownloaded("", "this-is-url", "")),
                NotificationState.Processing("this-is-url")
            ),
            Arguments.of(
                ProcessState.NetworkError,
                NotificationState.Error(R.string.network_error)
            ),
            Arguments.of(
                ProcessState.UnknownError,
                NotificationState.Error(R.string.unexpected_error)
            ),
            Arguments.of(
                ProcessState.StorageError,
                NotificationState.Error(R.string.storage_error)
            ),
            Arguments.of(
                ProcessState.ParsingError,
                NotificationState.Error(R.string.parsing_error)
            ),
            Arguments.of(ProcessState.Finished, NotificationState.Finish),
        )
    }
}
