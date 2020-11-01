package org.fnives.tiktokdownloader.ui.main.queue

import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.StateOfVideosObservableUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.helper.junit.rule.InstantExecutorExtension
import org.fnives.tiktokdownloader.helper.junit.rule.MainDispatcherExtension
import org.fnives.tiktokdownloader.ui.shared.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.math.exp

@Suppress("TestFunctionName")
@ExtendWith(InstantExecutorExtension::class, MainDispatcherExtension::class)
class QueueViewModelTest {

    private lateinit var stateOfVideosConflatedBroadcastChannel: ConflatedBroadcastChannel<List<VideoState>>
    private lateinit var mockStateOfVideosObservableUseCase: StateOfVideosObservableUseCase
    private lateinit var mockAddVideoToQueueUseCase: AddVideoToQueueUseCase
    private lateinit var mockVideoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
    private lateinit var sut: QueueViewModel

    @BeforeEach
    fun setup() {
        stateOfVideosConflatedBroadcastChannel = ConflatedBroadcastChannel()
        mockStateOfVideosObservableUseCase = mock()
        whenever(mockStateOfVideosObservableUseCase.invoke()).doReturn(stateOfVideosConflatedBroadcastChannel.asFlow())
        mockAddVideoToQueueUseCase = mock()
        mockVideoDownloadingProcessorUseCase = mock()
        sut = QueueViewModel(mockStateOfVideosObservableUseCase, mockAddVideoToQueueUseCase, mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_WHEN_observing_THEN_no_downloads_item_is_emitted() {
        sut.downloads.test().assertNoValue()
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyZeroInteractions(mockAddVideoToQueueUseCase)
        verifyZeroInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_AND_observing_WHEN_emitting_a_emptyList_THEN_it_is_sent_out() {
        val expected = listOf<VideoState>()
        stateOfVideosConflatedBroadcastChannel.offer(expected)

        sut.downloads.test().assertValue(expected)
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyZeroInteractions(mockAddVideoToQueueUseCase)
        verifyZeroInteractions(mockVideoDownloadingProcessorUseCase)
    }

    @Test
    fun GIVEN_initialized_AND_observing_WHEN_emitting_two_list_THEN_both_are_sent_out_in_order() {
        val expected1 = listOf(VideoState.InPending(VideoInPending("a1","b1")))
        val expected2 = listOf(VideoState.InPending(VideoInPending("a2","b2")))
        val testObserver = sut.downloads.test()

        stateOfVideosConflatedBroadcastChannel.offer(expected1)
        stateOfVideosConflatedBroadcastChannel.offer(expected2)

        testObserver.assertHistorySize(2).assertHasValue()
            .assertValueHistory(expected1, expected2)
        verify(mockStateOfVideosObservableUseCase, times(1)).invoke()
        verifyNoMoreInteractions(mockStateOfVideosObservableUseCase)
        verifyZeroInteractions(mockAddVideoToQueueUseCase)
        verifyZeroInteractions(mockVideoDownloadingProcessorUseCase)
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
}