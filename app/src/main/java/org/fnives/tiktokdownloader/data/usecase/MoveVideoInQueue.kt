package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending

class MoveVideoInQueue(
    private val videoInPendingLocalSource: VideoInPendingLocalSource
) {

    operator fun invoke(videoInPending: VideoInPending, to: VideoInPending) {

    }
}