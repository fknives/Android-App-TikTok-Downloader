package org.fnives.tiktokdownloader.data.local.save.video

import android.content.ContentResolver
import android.os.Environment
import android.provider.MediaStore
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import java.io.File
import java.io.IOException

@Suppress("DEPRECATION")
class SaveVideoFileBelowApi29(private val resolver: ContentResolver) : SaveVideoFile {

    @Throws(NetworkException::class, IOException::class)
    override fun invoke(directory: String, fileName: String, videoInProcess: VideoInSavingIntoFile): String? {
        val externalDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        externalDirectory.mkdir()
        val videoDirectory = File(externalDirectory, directory)
        videoDirectory.mkdir()
        val videoFile = File(videoDirectory, fileName)
        videoFile.createNewFile()
        videoInProcess.byteStream.safeCopyInto(videoFile.outputStream())

        val values = buildDefaultVideoContentValues(
            fileName = fileName,
            contentType = videoInProcess.contentType?.toString()
        ) {
            put(MediaStore.Video.Media.DATA, videoFile.path)
        }

        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.toString()
    }
}