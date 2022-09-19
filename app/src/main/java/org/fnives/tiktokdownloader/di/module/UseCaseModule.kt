package org.fnives.tiktokdownloader.di.module

import kotlinx.coroutines.Dispatchers
import org.fnives.tiktokdownloader.data.local.persistent.UserPreferencesStorage
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.GetUserPreferences
import org.fnives.tiktokdownloader.data.usecase.ObserveUserPreferences
import org.fnives.tiktokdownloader.data.usecase.MoveVideoInQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.RemoveVideoFromQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.SetUserPreferences
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

    val getUserPreferences: GetUserPreferences get() = GetUserPreferences(localSourceModule.userPreferencesLocalSource)
    val observeUserPreferences: ObserveUserPreferences get() = ObserveUserPreferences(localSourceModule.userPreferencesLocalSource)
    val setUserPreferences: SetUserPreferences get() = SetUserPreferences(localSourceModule.userPreferencesLocalSource)

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