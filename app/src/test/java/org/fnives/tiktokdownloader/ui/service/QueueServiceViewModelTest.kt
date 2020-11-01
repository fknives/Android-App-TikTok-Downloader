package org.fnives.tiktokdownloader.ui.service

import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.helper.junit.rule.InstantExecutorExtension
import org.fnives.tiktokdownloader.helper.junit.rule.MainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("TestFunctionName")
@ExtendWith(InstantExecutorExtension::class, MainDispatcherExtension::class)
class QueueServiceViewModelTest {

    private lateinit var videoDownloadingProcessorChannel: ConflatedBroadcastChannel<ProcessState>
    private lateinit var mockVideoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
    private lateinit var mockAddVideoToQueueUseCase: AddVideoToQueueUseCase
    private lateinit var sut: QueueServiceViewModel

    @BeforeEach
    fun setup() {
        videoDownloadingProcessorChannel = ConflatedBroadcastChannel()
        mockVideoDownloadingProcessorUseCase = mock()
        whenever(mockVideoDownloadingProcessorUseCase.processState).doReturn(videoDownloadingProcessorChannel.asFlow())
        mockAddVideoToQueueUseCase = mock()
        sut = QueueServiceViewModel(mockAddVideoToQueueUseCase, mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_valid_url_received_THEN_addVideoToQueue_is_called() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.onUrlReceived("url.com")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_invalid_url_received_THEN_addVideoToQueue_is_called_and_finished_emited() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(false)
        sut.onUrlReceived("url.com")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verifyZeroInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_more_url_received_THEN_addVideoToQueue_is_called_with_all() {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.onUrlReceived("url.com")
        sut.onUrlReceived("other.org")

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("url.com")
        verify(mockAddVideoToQueueUseCase, times(1)).invoke("other.org")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(2)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @ParameterizedTest(name = "GIVEN_initialized_URL_added_WHEN_{0}_sent_THEN_notificationState_is_{1}")
    @MethodSource("processStateObserveState")
    fun GIVEN_initialized_URL_added_WHEN_SpecificProcessState_sent_THEN_notificationState_is_correct(
        processState: ProcessState,
        expected: NotificationState
    ) {
        whenever(mockAddVideoToQueueUseCase.invoke(anyOrNull())).doReturn(true)
        sut.onUrlReceived("")
        videoDownloadingProcessorChannel.offer(processState)

        sut.notificationState.test()
            .assertHistorySize(1)
            .assertValue(expected)
    }

    @Test
    fun GIVEN_onCleared_WHEN_item_is_emited_THEN_it_is_no_longer_observed_and_cannot_be() {
        sut.onClear()

        sut.onUrlReceived("alma")
        videoDownloadingProcessorChannel.offer(ProcessState.UnknownError)

        sut.notificationState.test().assertNoValue()
    }

    companion object {
        @JvmStatic
        private fun processStateObserveState() = Stream.of(
            Arguments.of(
                ProcessState.Processing(VideoInPending("", "this-is-url")),
                NotificationState.Processing("this-is-url")
            ),
            Arguments.of(
                ProcessState.Processed(VideoDownloaded("", "this-is-url", "")),
                NotificationState.Processing("this-is-url")
            ),
            Arguments.of(ProcessState.NetworkError, NotificationState.Error(R.string.network_error)),
            Arguments.of(ProcessState.UnknownError, NotificationState.Error(R.string.unexpected_error)),
            Arguments.of(ProcessState.StorageError, NotificationState.Error(R.string.storage_error)),
            Arguments.of(ProcessState.ParsingError, NotificationState.Error(R.string.parsing_error)),
            Arguments.of(ProcessState.Finished, NotificationState.Finish),
        )
    }
}