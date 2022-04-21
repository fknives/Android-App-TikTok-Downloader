package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.fnives.tiktokdownloader.data.local.exceptions.StorageException
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.addTimeAtStart
import org.fnives.tiktokdownloader.data.local.persistent.getTimeAndOriginal
import org.fnives.tiktokdownloader.data.local.persistent.joinNormalized
import org.fnives.tiktokdownloader.data.local.persistent.separateIntoDenormalized
import org.fnives.tiktokdownloader.data.local.save.video.SaveVideoFile
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExists
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile

class VideoDownloadedLocalSource(
    private val saveVideoFile: SaveVideoFile,
    private val sharedPreferencesManagerImpl: SharedPreferencesManager,
    private val verifyFileForUriExists: VerifyFileForUriExists
) {

    val savedVideos: Flow<List<VideoDownloaded>>
        get() = sharedPreferencesManagerImpl.downloadedVideosFlow.map { stringSet ->
            val currentStored = stringSet.map { savedText ->
                val (time, data) = savedText.getTimeAndOriginal()
                Triple(savedText, time, data)
            }
                .sortedByDescending { it.second }
                .map { it.first to it.third.asVideoDownloaded() }

            val filtered = currentStored.filter { verifyFileForUriExists(it.second.uri) }
            if (currentStored != filtered) {
                sharedPreferencesManagerImpl.downloadedVideos = filtered.map { it.first }.toSet()
            }
            filtered.map { it.second }
        }
            .distinctUntilChanged()

    @Throws(StorageException::class)
    fun saveVideo(videoInProcess: VideoInSavingIntoFile): VideoDownloaded {
        val fileName = videoInProcess.fileName()
        val uri = try {
            saveVideoFile("TikTok_Downloader", fileName, videoInProcess)
        } catch (throwable: Throwable) {
            throw StorageException(cause = throwable)
        }
        uri ?: throw StorageException("Uri couldn't be created")

        val result = VideoDownloaded(id = videoInProcess.id, url = videoInProcess.url, uri = uri)
        saveVideoDownloaded(result)

        return result
    }

    private fun saveVideoDownloaded(videoDownloaded: VideoDownloaded) {
        sharedPreferencesManagerImpl.downloadedVideos = sharedPreferencesManagerImpl.downloadedVideos
            .plus(videoDownloaded.asString().addTimeAtStart())
    }

    fun removeVideo(videoDownloaded: VideoDownloaded) {
        sharedPreferencesManagerImpl.downloadedVideos = sharedPreferencesManagerImpl.downloadedVideos
            .filterNot { it.getTimeAndOriginal().second.asVideoDownloaded() == videoDownloaded }
            .toSet()
    }

    companion object {

        private fun VideoDownloaded.asString(): String =
            listOf(id, url, uri).joinNormalized()

        private fun String.asVideoDownloaded(): VideoDownloaded =
            separateIntoDenormalized().let { (id, url, uri) ->
                VideoDownloaded(id = id, url = url, uri = uri)
            }

        private fun VideoInSavingIntoFile.fileName() = "$id.${contentType?.subType ?: "mp4"}"
    }
}