package org.fnives.tiktokdownloader.ui.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.HasPendingVideosUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase

class QueueServiceViewModel(
    private val addVideoToQueueUseCase: AddVideoToQueueUseCase,
    private val hasPendingVideosUseCase: HasPendingVideosUseCase,
    private val videoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
) {

    private val parentJob = Job()
    private val viewModelScope = CoroutineScope(parentJob + Dispatchers.Main)
    private var collectionJob: Job? = null

    val notificationState: StateFlow<NotificationState?>
        field = MutableStateFlow(null)

    fun fetch(url: String?) {
        url?.let(addVideoToQueueUseCase::invoke)
        videoDownloadingProcessorUseCase.fetchVideoInState()
        if (collectionJob != null) return
        collectionJob = viewModelScope.launch {
            merge(
                hasPendingVideosUseCase().filter { !it }.map { NotificationState.Finish },
                videoDownloadingProcessorUseCase.processEvents.map(::mapState)
            ).collect {
                notificationState.value = it
            }
        }
    }

    fun onClear() {
        parentJob.cancel()
    }

    companion object {

        private fun mapState(state: ProcessState) =
            when (state) {
                is ProcessState.Processing ->
                    NotificationState.Processing(state.videoInPending.url)

                is ProcessState.Processed ->
                    NotificationState.Processing(state.videoDownloaded.url)

                ProcessState.NetworkError ->
                    NotificationState.Error(R.string.network_error)

                ProcessState.ParsingError ->
                    NotificationState.Error(R.string.parsing_error)

                ProcessState.StorageError ->
                    NotificationState.Error(R.string.storage_error)

                ProcessState.UnknownError ->
                    NotificationState.Error(R.string.unexpected_error)

                ProcessState.Finished -> NotificationState.Finish
                ProcessState.CaptchaError ->
                    NotificationState.Error(R.string.captcha_error)

                ProcessState.VideoDeletedError -> NotificationState.Error(R.string.video_deleted_error)
                ProcessState.VideoPrivateError -> NotificationState.Error(R.string.video_private_error)
            }
    }
}