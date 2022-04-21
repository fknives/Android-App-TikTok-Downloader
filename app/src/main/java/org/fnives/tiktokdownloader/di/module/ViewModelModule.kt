package org.fnives.tiktokdownloader.di.module

import androidx.lifecycle.SavedStateHandle
import org.fnives.tiktokdownloader.ui.main.MainViewModel
import org.fnives.tiktokdownloader.ui.main.queue.QueueViewModel
import org.fnives.tiktokdownloader.ui.service.QueueServiceViewModel

class ViewModelModule(private val useCaseModule: UseCaseModule) {

    val queueServiceViewModel: QueueServiceViewModel
        get() = QueueServiceViewModel(
            useCaseModule.addVideoToQueueUseCase,
            useCaseModule.videoDownloadingProcessorUseCase
        )

    fun mainViewModel(savedStateHandle: SavedStateHandle): MainViewModel =
        MainViewModel(
            useCaseModule.videoDownloadingProcessorUseCase,
            useCaseModule.addVideoToQueueUseCase,
            savedStateHandle
        )

    val queueViewModel: QueueViewModel
        get() = QueueViewModel(
            useCaseModule.stateOfVideosObservableUseCase,
            useCaseModule.addVideoToQueueUseCase,
            useCaseModule.removeVideoFromQueueUseCase,
            useCaseModule.videoDownloadingProcessorUseCase
        )
}