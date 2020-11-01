package org.fnives.tiktokdownloader.data.local.save.video

import android.content.ContentResolver
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media
import androidx.annotation.RequiresApi
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import java.io.FileOutputStream

class SaveVideoFileApi29(private val resolver: ContentResolver) : SaveVideoFile {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun invoke(directory: String, fileName: String, videoInProcess: VideoInSavingIntoFile): String? {
        val values = buildDefaultVideoContentValues(
            fileName = fileName,
            contentType = videoInProcess.contentType?.toString()
        ) {
            put(Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + directory)
            put(Media.DATE_TAKEN, System.currentTimeMillis())
            put(Media.IS_PENDING, IS_PENDING_YES)
        }

        val collection = Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uriSavedVideo = resolver.insert(collection, values)
        if (uriSavedVideo != null) {
            resolver.openFileDescriptor(uriSavedVideo, "w")
                ?.fileDescriptor
                ?.let(::FileOutputStream)
                ?.let(videoInProcess.byteStream::safeCopyInto)

            values.clear()
            values.put(Media.IS_PENDING, IS_PENDING_NO)
            resolver.update(uriSavedVideo, values, null, null)
        }
        return uriSavedVideo?.toString()
    }
}