package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoState

class RemoveVideoFromQueueUseCase(
    private val videoInPendingLocalSource: VideoInPendingLocalSource,
    private val videoDownloadedLocalSource: VideoDownloadedLocalSource
) {

    operator fun invoke(videoState: VideoState) {
        when(videoState) {
            is VideoState.Downloaded -> videoDownloadedLocalSource.removeVideo(videoState.videoDownloaded)
            is VideoState.InPending -> videoInPendingLocalSource.removeVideoFromQueue(videoState.videoInPending)
            is VideoState.InProcess -> Unit
        }
    }
}