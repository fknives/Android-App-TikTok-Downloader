package org.fnives.tiktokdownloader.data.local.save.video

import android.content.ContentResolver
import android.os.Build
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import java.io.IOException

interface SaveVideoFile {

    @Throws(IOException::class)
    operator fun invoke(directory: String, fileName: String, videoInProcess: VideoInSavingIntoFile): String?

    class Factory(private val contentResolver: ContentResolver) {

        fun create(): SaveVideoFile =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SaveVideoFileApi29(contentResolver)
            } else {
                SaveVideoFileBelowApi29(contentResolver)
            }
    }
}