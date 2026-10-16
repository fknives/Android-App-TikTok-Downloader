package org.fnives.tiktokdownloader.data.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource

/**
 * Emits whether there is still a video to process, either waiting in the queue or being downloaded.
 */
class HasPendingVideosUseCase(
    private val videoInPendingLocalSource: VideoInPendingLocalSource,
    private val videoInProgressLocalSource: VideoInProgressLocalSource
) {

    operator fun invoke(): Flow<Boolean> =
        combine(
            videoInPendingLocalSource.pendingVideos,
            videoInProgressLocalSource.videoInProcessFlow
        ) { pending, inProgress -> pending.isNotEmpty() || inProgress != null }
            .distinctUntilChanged()
}
