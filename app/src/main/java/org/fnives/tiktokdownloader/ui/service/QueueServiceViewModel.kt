package org.fnives.tiktokdownloader.ui.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.data.model.ProcessState

class QueueServiceViewModel(
    private val addVideoToQueueUseCase: AddVideoToQueueUseCase,
    private val videoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase
) {

    private val parentJob = Job()
    private val viewModelScope = CoroutineScope(parentJob + Dispatchers.Main)
    private var listeningJob: Job? = null

    private val _notificationState = MutableLiveData<NotificationState>()
    val notificationState: LiveData<NotificationState> = _notificationState

    fun onUrlReceived(url: String) {
        if (!addVideoToQueueUseCase(url)){
            return
        }
        if (listeningJob == null) {
            listeningJob = startListening()
        }
        videoDownloadingProcessorUseCase.fetchVideoInState()
    }

    private fun startListening(): Job = viewModelScope.launch {
        videoDownloadingProcessorUseCase.processState.collect {
            val value = when (it) {
                is ProcessState.Processing ->
                    NotificationState.Processing(it.videoInPending.url)
                is ProcessState.Processed ->
                    NotificationState.Processing(it.videoDownloaded.url)
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
            }
            _notificationState.postValue(value)
        }
    }

    fun onClear() {
        parentJob.cancel()
    }
}