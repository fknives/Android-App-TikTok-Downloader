package org.fnives.tiktokdownloader.data.local.verify.exists

import android.content.ContentResolver
import android.provider.BaseColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerifyFileForUriExistsImpl(
    private val contentResolver: ContentResolver
) : VerifyFileForUriExists {

    override suspend fun invoke(uri: String): Boolean = withContext(Dispatchers.IO) {
        true == contentResolver.query(uri.toUri(), arrayOf(BaseColumns._ID), null, null, null)
            ?.use { it.moveToFirst() }
    }
}