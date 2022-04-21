package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.addTimeAtStart
import org.fnives.tiktokdownloader.data.local.persistent.getTimeAndOriginal
import org.fnives.tiktokdownloader.data.local.persistent.joinNormalized
import org.fnives.tiktokdownloader.data.local.persistent.separateIntoDenormalized
import org.fnives.tiktokdownloader.data.model.VideoInPending

class VideoInPendingLocalSource(
    private val sharedPreferencesManager: SharedPreferencesManager,
) {
    val pendingVideos: Flow<List<VideoInPending>>
        get() = sharedPreferencesManager.pendingVideosFlow
            .map { stringSet ->
                stringSet.asSequence().map { timeThenUrl -> timeThenUrl.getTimeAndOriginal() }
                    .sortedBy { it.first }
                    .map { it.second }
                    .map { it.asVideoInPending() }
                    .toList()
            }

    fun saveUrlIntoQueue(videoInPending: VideoInPending) {
        sharedPreferencesManager.pendingVideos = sharedPreferencesManager.pendingVideos
            .plus(videoInPending.asString().addTimeAtStart())
    }

    fun removeVideoFromQueue(videoInPending: VideoInPending) {
        sharedPreferencesManager.pendingVideos = sharedPreferencesManager.pendingVideos
            .filterNot { it.getTimeAndOriginal().second.asVideoInPending() == videoInPending }
            .toSet()
    }

    companion object {
        private fun VideoInPending.asString(): String = listOf(id, url).joinNormalized()

        private fun String.asVideoInPending(): VideoInPending =
            separateIntoDenormalized().let { (id, url) ->
                VideoInPending(id = id, url = url)
            }
    }
}