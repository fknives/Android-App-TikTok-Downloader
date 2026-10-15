package org.fnives.tiktokdownloader.data.local.save.video

import android.content.ContentValues
import android.provider.MediaStore
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

@Throws(NetworkException::class, IOException::class)
fun InputStream.safeCopyInto(outputStream: OutputStream) {
    outputStream.use { out ->
        use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = try {
                    input.read(buffer)
                } catch (ioException: IOException) {
                    // The bytes are read from the network response, so a failure here is a network problem rather than a local storage problem.
                    throw NetworkException(message = "Couldn't read the response stream", html = "", cause = ioException)
                }
                if (bytesRead == -1) break
                out.write(buffer, 0, bytesRead)
            }
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