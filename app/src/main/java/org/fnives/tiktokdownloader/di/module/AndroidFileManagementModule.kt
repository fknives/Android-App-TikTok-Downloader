package org.fnives.tiktokdownloader.di.module

import android.content.ContentResolver
import android.content.Context
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManagerImpl
import org.fnives.tiktokdownloader.data.local.save.video.SaveVideoFile
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExists
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExistsImpl
import org.fnives.tiktokdownloader.di.ServiceLocator

class AndroidFileManagementModule(private val context: Context) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    val verifyFileForUriExists: VerifyFileForUriExists
        get() = VerifyFileForUriExistsImpl(contentResolver)

    val sharedPreferencesManager: SharedPreferencesManager by lazy {
        SharedPreferencesManagerImpl.create(context)
    }

    private val saveVideoFileFactory: SaveVideoFile.Factory
        get() = SaveVideoFile.Factory(contentResolver)

    val saveVideoFile: SaveVideoFile
        get() = saveVideoFileFactory.create()
}