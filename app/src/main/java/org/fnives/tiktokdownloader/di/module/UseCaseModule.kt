package org.fnives.tiktokdownloader.di.module

import kotlinx.coroutines.Dispatchers
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.MoveVideoInQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.RemoveVideoFromQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.StateOfVideosObservableUseCase
import org.fnives.tiktokdownloader.data.usecase.UrlVerificationUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase

class UseCaseModule(
    private val localSourceModule: LocalSourceModule,
    private val networkModule: NetworkModule
) {

    val stateOfVideosObservableUseCase: StateOfVideosObservableUseCase
        get() = StateOfVideosObservableUseCase(
            videoInPendingLocalSource = localSourceModule.videoInPendingLocalSource,
            videoDownloadedLocalSource = localSourceModule.videoDownloadedLocalSource,
            videoInProgressLocalSource = localSourceModule.videoInProgressLocalSource,
            dispatcher = Dispatchers.IO
        )

    private val urlVerificationUseCase: UrlVerificationUseCase
        get() = UrlVerificationUseCase()

    val addVideoToQueueUseCase: AddVideoToQueueUseCase
        get() = AddVideoToQueueUseCase(
            urlVerificationUseCase,
            localSourceModule.videoInPendingLocalSource
        )

    val removeVideoFromQueueUseCase: RemoveVideoFromQueueUseCase
        get() = RemoveVideoFromQueueUseCase(
            localSourceModule.videoInPendingLocalSource,
            localSourceModule.videoDownloadedLocalSource
        )

    val moveVideoInQueueUseCase: MoveVideoInQueueUseCase
        get() = MoveVideoInQueueUseCase(
            localSourceModule.videoInPendingLocalSource
        )

    val videoDownloadingProcessorUseCase: VideoDownloadingProcessorUseCase by lazy {
        VideoDownloadingProcessorUseCase(
            tikTokDownloadRemoteSource = networkModule.tikTokDownloadRemoteSource,
            videoInPendingLocalSource = localSourceModule.videoInPendingLocalSource,
            videoDownloadedLocalSource = localSourceModule.videoDownloadedLocalSource,
            videoInProgressLocalSource = localSourceModule.videoInProgressLocalSource,
            captchaTimeoutLocalSource = localSourceModule.captchaTimeoutLocalSource,
            dispatcher = Dispatchers.IO
        )
    }
}