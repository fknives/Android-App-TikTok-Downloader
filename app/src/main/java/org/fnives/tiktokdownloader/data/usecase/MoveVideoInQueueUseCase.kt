package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending

class MoveVideoInQueueUseCase(
    private val videoInPendingLocalSource: VideoInPendingLocalSource
) {

    operator fun invoke(videoInPending: VideoInPending, positionDifference: Int) {
        videoInPendingLocalSource.moveBy(videoInPending, positionDifference)
    }
}