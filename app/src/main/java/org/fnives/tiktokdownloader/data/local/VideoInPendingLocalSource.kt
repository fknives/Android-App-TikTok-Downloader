package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.addTimeAtStart
import org.fnives.tiktokdownloader.data.local.persistent.getTimeAndOriginal
import org.fnives.tiktokdownloader.data.local.persistent.joinNormalized
import org.fnives.tiktokdownloader.data.local.persistent.separateIntoDenormalized
import org.fnives.tiktokdownloader.data.model.VideoInPending
import kotlin.math.abs

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

    fun moveBy(videoInPending: VideoInPending, positionDifference: Int) {
        if (positionDifference == 0) return

        val mutableOrdered = sharedPreferencesManager.pendingVideos
            .map { it.getTimeAndOriginal() }
            .sortedBy { it.first }
            .toMutableList()

        val index = mutableOrdered.indexOfFirst { it.second.asVideoInPending() == videoInPending }
        val endTime = mutableOrdered[index + positionDifference].first

        val direction = -positionDifference / abs(positionDifference)
        val range = IntProgression.fromClosedRange(index + positionDifference, index - direction, direction)
        range.forEach {
            mutableOrdered[it] = mutableOrdered[it + direction].first to mutableOrdered[it].second
        }
        mutableOrdered[index] = endTime to mutableOrdered[index].second

        sharedPreferencesManager.pendingVideos = mutableOrdered.map { it.second.addTimeAtStart(it.first) }.toSet()
    }

    companion object {
        private fun VideoInPending.asString(): String = listOf(id, url).joinNormalized()

        private fun String.asVideoInPending(): VideoInPending =
            separateIntoDenormalized().let { (id, url) ->
                VideoInPending(id = id, url = url)
            }
    }
}