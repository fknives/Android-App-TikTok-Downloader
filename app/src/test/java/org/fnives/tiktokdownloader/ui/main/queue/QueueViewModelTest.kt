package org.fnives.tiktokdownloader.ui.main.queue

import com.jraska.livedata.test
import kotlinx.coroutines.flow.MutableSharedFlow
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.fnives.tiktokdownloader.data.model.VideoState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.MoveVideoInQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.RemoveVideoFromQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.StateOfVideosObservableUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.helper.junit.rule.InstantExecutorExtension
import org.fnives.tiktokdownloader.helper.junit.rule.MainDispatcherExtension
import org.fnives.tiktokdownloader.ui.shared.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@Suppress("TestFunctionName")
@ExtendWith(InstantExecutorExtension::class, MainDispatcherExtension::class)
class QueueViewModelTest {

    private lateinit var stateOfVideosFlow: MutableSharedFlow<List<VideoState>>
    private lateinit var mockStateOfVideosObservableUseCase: StateOfVideosObservableUseCase
    private lateinit var mockAddVideoToQueueUseCase: AddVideoToQueueUseCase
    private lateinit var mockRemoveVideoFromQueueUseCase: RemoveVideoFromQueueUseCase
    private lateinit var mockVideoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
    private lateinit var mockMoveVideoInQueueUseCase: MoveVideoInQueueUseCase
    private lateinit var sut: QueueViewModel

    @BeforeEach
    fun setup() {
        stateOfVideosFlow = MutableSharedFlow(replay = 1)
        mockStateOfVideosObservableUseCase = mock()
        whenever(mockStateOfVideosObservableUseCase.invoke()).doReturn(stateOfVideosFlow)
        mockAddVideoToQueueUseCase = mock()
        mockRemoveVideoFromQueueUseCase = mock()
        mockVideoDownloadingProcessorUseCase = mock()
        mockMoveVideoInQueueUseCase = mock()
        sut = QueueViewModel(
            mockStateOfVideosObservableUseCase,
            mockAddVideoToQueueUseCase,
            mockRemoveVideoFromQueueUseCase,
            mockVideoDownloadingProcessorUseCase,
            mockMoveVideoInQueueUseCase
        )
    }

    @Test
    fun GIVEN_initialized_WHEN_observing_THEN_no_downloads_item_is_emitted() {
        sut.downloads.test().assertNoValue()
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_AND_observing_WHEN_emitting_a_emptyList_THEN_it_is_sent_out() {
        val expected = listOf<VideoState>()
        stateOfVideosFlow.tryEmit(expected)

        sut.downloads.test().assertValue(expected)
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_AND_observing_WHEN_emitting_two_list_THEN_both_are_sent_out_in_order() {
        val expected1 = listOf(VideoState.InPending(VideoInPending("a1", "b1")))
        val expected2 = listOf(VideoState.InPending(VideoInPending("a2", "b2")))
        val testObserver = sut.downloads.test()

        stateOfVideosFlow.tryEmit(expected1)
        stateOfVideosFlow.tryEmit(expected2)

        testObserver.assertHistorySize(2).assertHasValue()
            .assertValueHistory(expected1, expected2)
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_onSaveClicked_THEN_addVideoToQueueUseCase_is_called() {
        sut.onSaveClicked("alma.com")

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verify(mockAddVideoToQueueUseCase, times(1)).invoke("alma.com")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(1)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_onSaveClicked_twice_THEN_addVideoToQueueUseCase_is_called_twice() {
        sut.onSaveClicked("alma.com")
        sut.onSaveClicked("banan.org")

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verify(mockAddVideoToQueueUseCase, times(1)).invoke("alma.com")
        verify(mockAddVideoToQueueUseCase, times(1)).invoke("banan.org")
        verifyNoMoreInteractions(mockAddVideoToQueueUseCase)
        verify(mockVideoDownloadingProcessorUseCase, times(2)).fetchVideoInState()
        verifyNoMoreInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_THEN_no_navigation_event_is_sent_out() {
        sut.navigationEvent.test().assertNoValue()
    }

    @Test
    fun GIVEN_initialized_WHEN_url_clicked_THEN_navigation_event_is_sent_out() {
        sut.onUrlClicked("alma.com")
        sut.navigationEvent.test()
            .assertHistorySize(1)
            .assertValue(Event(QueueViewModel.NavigationEvent.OpenBrowser("alma.com")))
    }

    @Test
    fun GIVEN_initialized_WHEN_item_clicked_THEN_navigation_event_is_sent_out() {
        sut.onItemClicked("alma.com")
        sut.navigationEvent.test()
            .assertHistorySize(1)
            .assertValue(Event(QueueViewModel.NavigationEvent.OpenGallery("alma.com")))
    }

    @Test
    fun GIVEN_initialized_WHEN_inProgress_onElementDeleted_THEN_remove_called() {
        val arg = VideoState.InProcess(VideoInProgress("1","2"))
        sut.onElementDeleted(arg)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verify(mockRemoveVideoFromQueueUseCase, times(1)).invoke(arg)
        verifyNoMoreInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_pending_onElementDeleted_THEN_remove_called() {
        val arg = VideoState.InPending(VideoInPending("1","2"))
        sut.onElementDeleted(arg)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verify(mockRemoveVideoFromQueueUseCase, times(1)).invoke(arg)
        verifyNoMoreInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_download_onElementDeleted_THEN_remove_called() {
        val arg = VideoState.Downloaded(VideoDownloaded("1","2","3"))
        sut.onElementDeleted(arg)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verify(mockRemoveVideoFromQueueUseCase, times(1)).invoke(arg)
        verifyNoMoreInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_in_progress_moved_THEN_nothing_changes() {
        val arg = VideoState.InProcess(VideoInProgress("1","2"))
        sut.onElementMoved(arg, 100)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_done_moved_THEN_nothing_changes() {
        val arg = VideoState.Downloaded(VideoDownloaded("1","2","3"))
        sut.onElementMoved(arg, 100)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verifyNoInteractions(mockMoveVideoInQueueUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_pending_moved_THEN_nothing_changes() {
        val arg = VideoState.InPending(VideoInPending("1","2"))
        sut.onElementMoved(arg, 100)

        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyNoInteractions(mockAddVideoToQueueUseCase)
        verifyNoInteractions(mockVideoDownloadingProcessorUseCase)
        verifyNoInteractions(mockRemoveVideoFromQueueUseCase)
        verify(mockMoveVideoInQueueUseCase, times(1)).invoke(arg.videoInPending, 100)
        verifyNoMoreInteractions(mockMoveVideoInQueueUseCase)
    }
}