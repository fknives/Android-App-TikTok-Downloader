package org.fnives.tiktokdownloader.errortracking

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object SendErrorsAsEmail {

    fun send(context: Context) {
        val subfolder = File(context.cacheDir, "errors")
        if (!subfolder.exists()) {
            subfolder.mkdirs()
        }

        // Create the file inside subfolder
        val tempFile = File(subfolder, "errors.json")

        // Write content
        tempFile.writeText(ErrorTracer.getErrorAsJSON())
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.setType("application/json")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("projectsupport202@proton.me"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporting Errors from TikTokDownloader")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Attached error as JSON")
        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(Intent.createChooser(emailIntent, "Report errors in email..."))
    }
}