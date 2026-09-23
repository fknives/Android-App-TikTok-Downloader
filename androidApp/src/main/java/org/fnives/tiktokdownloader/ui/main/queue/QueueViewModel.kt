package org.fnives.tiktokdownloader.ui.main.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.fnives.tiktokdownloader.data.model.VideoState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.MoveVideoInQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.RemoveVideoFromQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.StateOfVideosObservableUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.ui.shared.Event

class QueueViewModel(
    stateOfVideosObservableUseCase: StateOfVideosObservableUseCase,
    private val addVideoToQueueUseCase: AddVideoToQueueUseCase,
    private val removeVideoFromQueueUseCase: RemoveVideoFromQueueUseCase,
    private val videoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase,
    private val moveVideoInQueueUseCase: MoveVideoInQueueUseCase
) : ViewModel() {

    val downloads: StateFlow<List<VideoState>> = stateOfVideosObservableUseCase().stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = listOf()
    )
    private val _navigationEvent = MutableStateFlow<Event<NavigationEvent>?>(null)
    val navigationEvent: StateFlow<Event<NavigationEvent>?> = _navigationEvent

    fun onSaveClicked(url: String) {
        addVideoToQueueUseCase(url)
        videoDownloadingProcessorUseCase.fetchVideoInState()
    }

    fun onItemClicked(path: String) {
        _navigationEvent.value = Event(NavigationEvent.OpenGallery(path))
    }

    fun onUrlClicked(url: String) {
        _navigationEvent.value = Event(NavigationEvent.OpenBrowser(url))
    }

    fun onElementDeleted(videoState: VideoState) {
        removeVideoFromQueueUseCase(videoState)
    }

    fun onElementMoved(moved: VideoState, positionDifference: Int): Boolean {
        if (moved !is VideoState.InPending) return false
        moveVideoInQueueUseCase(moved.videoInPending, positionDifference)
        return true
    }

    sealed class NavigationEvent {
        data class OpenBrowser(val url: String) : NavigationEvent()
        data class OpenGallery(val uri: String) : NavigationEvent()
    }
}