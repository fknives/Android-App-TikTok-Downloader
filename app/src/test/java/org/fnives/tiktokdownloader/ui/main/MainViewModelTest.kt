package org.fnives.tiktokdownloader.ui.main

import androidx.lifecycle.SavedStateHandle
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.helper.junit.rule.InstantExecutorExtension
import org.fnives.tiktokdownloader.helper.junit.rule.MainDispatcherExtension
import org.fnives.tiktokdownloader.ui.shared.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("TestFunctionName")
@ExtendWith(InstantExecutorExtension::class, MainDispatcherExtension::class)
class MainViewModelTest {

    private lateinit var conflatedBroadcastChannel: ConflatedBroadcastChannel<ProcessState>
    private lateinit var mockVideoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
    private lateinit var mockAddVideoToQueueUseCase: AddVideoToQueueUseCase
    private lateinit var sut: MainViewModel

    @BeforeEach
    fun setup() {
        conflatedBroadcastChannel = ConflatedBroadcastChannel()
        mockVideoDownloadingProcessorUseCase = mock()
        mockAddVideoToQueueUseCase = mock()
        whenever(mockVideoDownloadingProcessorUseCase.processState).doReturn(conflatedBroadcastChannel.asFlow())
        sut = MainViewModel(mockVideoDownloadingProcessorUseCase, mockAddVideoToQueueUseCase, SavedStateHandle())
    }

    @Test
    fun GIVEN_url_in_savedStateHandle_THEN_its_saved_into_addVideoToQueue() {
        val savedStateHandle = mock<SavedStateHandle>()
        whenever(savedStateHandle.get<String>(MainActivity.INTENT_EXTRA_URL)).doReturn("alma.c")

        MainViewModel(mockVideoDownloadingProcessorUseCase, mockAddVideoToQueueUseCase, savedStateHandle)

        verify(mockAddVideoToQueueUseCase, times(1)).invoke("alma.c")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(savedStateHandle, times(1)).get<String>(MainActivity.INTENT_EXTRA_URL)
        verify(savedStateHandle, times(1)).set(MainActivity.INTENT_EXTRA_URL, null)
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
    }

    @Test
    fun GIVEN_onScreenSet_Queue_WHEN_observing_refresh_action_THEN_it_is_false() {
        sut.onScreenSelected(MainViewModel.Screen.QUEUE)

        sut.refreshActionVisibility.test()
            .assertHasValue()
            .assertValue(false)

        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_onScreenSet_Help_WHEN_observing_refreshAction_THEN_it_is_false() {
        sut.onScreenSelected(MainViewModel.Screen.HELP)

        sut.refreshActionVisibility.test()
            .assertHasValue()
            .assertValue(false)

        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @ParameterizedTest(name = "GIVEN_{0}_WHEN_observing_refreshAction_AND_{1}_THEN_its_{2}")
    @MethodSource("refreshActionParameters")
    fun GIVEN_SpecificScreen_WHEN_observing_refreshAction_AND_SpecificProcessState_THEN_its_correct(
        screen: MainViewModel.Screen,
        processState: ProcessState,
        expected: Boolean
    ) {
        sut.onScreenSelected(screen)

        val testObserver = sut.refreshActionVisibility.test()

        conflatedBroadcastChannel.offer(processState)

        testObserver.assertHistorySize(2).assertValueHistory(false, expected)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @ParameterizedTest(name = "GIVEN_{0}_WHEN_observing_refreshAction_AND_{1}_THEN_its_{2}")
    @MethodSource("errorMEssageParameters")
    fun GIVEN_SpecificScreen_WHEN_observing_errorMessage_AND_SpecificProcessState_THEN_its_correct(
        screen: MainViewModel.Screen,
        processState: ProcessState,
        expected: MainViewModel.ErrorMessage?
    ) {
        sut.onScreenSelected(screen)

        val testObserver = sut.errorMessage.test()

        conflatedBroadcastChannel.offer(processState)

        testObserver.assertHistorySize(2).assertValueHistory(null, expected?.let(::Event))
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_onScreenSet_Queue_WHEN_observing_errorMessage_THEN_its_null() {
        sut.onScreenSelected(MainViewModel.Screen.QUEUE)

        sut.errorMessage.test()
            .assertHasValue()
            .assertValue(null)
    }

    @Test
    fun GIVEN_onScreenSet_Help_WHEN_observing_errorMessage_THEN_its_null() {
        sut.onScreenSelected(MainViewModel.Screen.HELP)

        sut.errorMessage.test()
            .assertHasValue()
            .assertValue(null)
    }

    @Test
    fun GIVEN_initialized_WHEN_onFetchDownloadClicked_is_called_THEN_processor_is_notified() {
        sut.onFetchDownloadClicked()

        verify(mockVideoDownloadingProcessorUseCase, times(2)).fetchVideoInState()
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_onFetchDownloadClicked_is_called_multiple_times_THEN_processor_is_notified() {
        sut.onFetchDownloadClicked()
        sut.onFetchDownloadClicked()
        sut.onFetchDownloadClicked()

        verify(mockVideoDownloadingProcessorUseCase, times(4)).fetchVideoInState()
        verify(mockVideoDownloadingProcessorUseCase, times(1)).processState
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
    }

    companion object {

        @JvmStatic
        private fun refreshActionParameters() = Stream.of(
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Processing(VideoInPending("", "")), false),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Processed(VideoDownloaded("", "", "")), false),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Finished, false),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.NetworkError, true),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.ParsingError, true),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.StorageError, true),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.UnknownError, true),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Processing(VideoInPending("", "")), false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Processed(VideoDownloaded("", "", "")), false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Finished, false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.NetworkError, false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.ParsingError, false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.StorageError, false),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.UnknownError, false)
        )

        @JvmStatic
        private fun errorMEssageParameters() = Stream.of(
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Processing(VideoInPending("", "")), null),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Processed(VideoDownloaded("", "", "")), null),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.Finished, null),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.NetworkError, MainViewModel.ErrorMessage.NETWORK),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.ParsingError, MainViewModel.ErrorMessage.PARSING),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.StorageError, MainViewModel.ErrorMessage.STORAGE),
            Arguments.of(MainViewModel.Screen.QUEUE, ProcessState.UnknownError, MainViewModel.ErrorMessage.UNKNOWN),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Processing(VideoInPending("", "")), null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Processed(VideoDownloaded("", "", "")), null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.Finished, null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.NetworkError, null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.ParsingError, null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.StorageError, null),
            Arguments.of(MainViewModel.Screen.HELP, ProcessState.UnknownError, null)
        )
    }
}