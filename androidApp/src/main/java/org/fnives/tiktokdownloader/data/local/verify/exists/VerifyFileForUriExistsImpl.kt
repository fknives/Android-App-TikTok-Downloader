package org.fnives.tiktokdownloader.data.local.verify.exists

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerifyFileForUriExistsImpl(
    private val contentResolver: ContentResolver
) : VerifyFileForUriExists {

    override suspend fun invoke(uris: Collection<String>): Set<String> =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext emptySet()

            uris.groupBy { it.substringBeforeLast(COLLECTION_SEPARATOR) }
                .flatMap { (collection, urisInCollection) ->
                    val idToUri = urisInCollection.mapNotNull { uri ->
                        runCatching { ContentUris.parseId(uri.toUri()) }.getOrNull()
                            ?.let { it to uri }
                    }

                    val presentIds =
                        idToUri.map { it.first }
                            .chunked(MAX_IDS_PER_QUERY)
                            .flatMap { chunk -> getPresentIds(collection.toUri(), chunk) }
                            .toSet()

                    idToUri.mapNotNull { (id, uri) -> uri.takeIf { id in presentIds } }
                }
                .toSet()
        }

    private suspend fun getPresentIds(collection: Uri, elementsToLookFor: List<Long>): Set<Long> = withContext(Dispatchers.IO) {
        if (elementsToLookFor.isEmpty()) return@withContext emptySet()

        val verified = mutableSetOf<Long>()
        contentResolver.query(
            collection,
            arrayOf(BaseColumns._ID),
            "${BaseColumns._ID} IN (${elementsToLookFor.joinToString(",")})",
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            while (cursor.moveToNext()) {
                verified.add(cursor.getLong(idColumn))
            }
        }
        return@withContext verified
    }

    companion object {
        private const val COLLECTION_SEPARATOR = '/'

        private const val MAX_IDS_PER_QUERY = 400
    }
}
