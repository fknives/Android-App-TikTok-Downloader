package org.fnives.tiktokdownloader.di.module

import android.content.ContentResolver
import android.content.Context
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManagerImpl
import org.fnives.tiktokdownloader.data.local.persistent.UserPreferencesStorage
import org.fnives.tiktokdownloader.data.local.save.video.SaveVideoFile
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExists
import org.fnives.tiktokdownloader.data.local.verify.exists.VerifyFileForUriExistsImpl

class AndroidFileManagementModule(private val context: Context) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    val verifyFileForUriExists: VerifyFileForUriExists
        get() = VerifyFileForUriExistsImpl(contentResolver)

    private val sharedPreferencesManagerImpl by lazy {
        SharedPreferencesManagerImpl.create(context)
    }

    val sharedPreferencesManager: SharedPreferencesManager get() = sharedPreferencesManagerImpl

    val userPreferencesStorage: UserPreferencesStorage get() = sharedPreferencesManagerImpl

    private val saveVideoFileFactory: SaveVideoFile.Factory
        get() = SaveVideoFile.Factory(contentResolver)

    val saveVideoFile: SaveVideoFile
        get() = saveVideoFileFactory.create()
}