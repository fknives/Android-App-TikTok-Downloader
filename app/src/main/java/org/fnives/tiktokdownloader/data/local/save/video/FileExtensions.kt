package org.fnives.tiktokdownloader.data.local.save.video

import android.content.ContentValues
import android.provider.MediaStore
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

fun InputStream.safeCopyInto(outputStream: OutputStream) {
    outputStream.use { out ->
        use { input ->
            input.copyTo(out)
        }
    }
}

fun buildDefaultVideoContentValues(
    fileName: String,
    contentType: String?,
    update: ContentValues.() -> Unit
): ContentValues {
    val contentValues = ContentValues()
    update(contentValues)
    contentValues.put(MediaStore.Video.Media.TITLE, fileName)
    contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
    contentValues.put(MediaStore.Video.Media.MIME_TYPE, contentType)
    val dateAddedInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    contentValues.put(MediaStore.Video.Media.DATE_ADDED, dateAddedInSeconds)

    return contentValues
}

const val IS_PENDING_YES = 1
const val IS_PENDING_NO = 0