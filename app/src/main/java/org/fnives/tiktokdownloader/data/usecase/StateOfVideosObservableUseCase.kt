package org.fnives.tiktokdownloader.data.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.fnives.tiktokdownloader.data.model.VideoState

@OptIn(FlowPreview::class)
class StateOfVideosObservableUseCase(
    videoInProgressLocalSource: VideoInProgressLocalSource,
    videoInPendingLocalSource: VideoInPendingLocalSource,
    videoDownloadedLocalSource: VideoDownloadedLocalSource,
    private val dispatcher: CoroutineDispatcher
) {

    private val videoStateFlow by lazy {
        combine(
            videoInProgressLocalSource.videoInProcessFlow,
            videoInPendingLocalSource.pendingVideos,
            videoDownloadedLocalSource.savedVideos,
            ::combineTogether
        )
            .debounce(WORK_FLOW_DEBOUNCE)
            .distinctUntilChanged()
            .flowOn(dispatcher)
    }

    operator fun invoke(): Flow<List<VideoState>> = videoStateFlow

    private suspend fun combineTogether(
        videoInProgress: VideoInProgress?,
        pendingVideos: List<VideoInPending>,
        downloaded: List<VideoDownloaded>
    ): List<VideoState> = withContext(dispatcher) {
        val result = mutableListOf<VideoState>()
        if (videoInProgress != null) {
            result.add(VideoState.InProcess(videoInProgress))
        }
        result.addAll(pendingVideos.filter { it.url != videoInProgress?.url }
            .map(VideoState::InPending))
        result.addAll(downloaded.map(VideoState::Downloaded))

        return@withContext result
    }

    companion object {
        private const val WORK_FLOW_DEBOUNCE = 200L
    }
}