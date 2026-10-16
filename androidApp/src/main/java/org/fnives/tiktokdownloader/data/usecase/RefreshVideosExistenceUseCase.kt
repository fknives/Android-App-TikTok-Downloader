package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource

/**
 * Re-checks the stored videos and prunes the ones whose file no longer exists. Meant to run when
 * the app returns to the foreground, since files may have been removed while it was away.
 */
class RefreshVideosExistenceUseCase(
    private val videoDownloadedLocalSource: VideoDownloadedLocalSource
) {

    suspend operator fun invoke() {
        videoDownloadedLocalSource.refreshStoredVideos()
    }
}
