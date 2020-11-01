package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending
import java.util.*

class AddVideoToQueueUseCase(
    private val urlVerificationUseCase: UrlVerificationUseCase,
    private val videoInPendingLocalSource: VideoInPendingLocalSource
) {

    operator fun invoke(url: String) : Boolean {
        if (!urlVerificationUseCase(url)) {
            return false
        }
        val newVideoInPending = VideoInPending(id = UUID.randomUUID().toString(), url = url)
        videoInPendingLocalSource.saveUrlIntoQueue(newVideoInPending)

        return true
    }
}