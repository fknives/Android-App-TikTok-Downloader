package org.fnives.tiktokdownloader.data.local.verify.exists

import java.util.concurrent.ConcurrentHashMap

/**
 * Caches the uris that were verified to exist, so repeated checks don't hit the ContentResolver again.
 */
class CachingVerifyFileForUriExists(
    private val delegate: VerifyFileForUriExists
) : VerifyFileForUriExists, StoredVideosCacheInvalidation {

    private val knownExisting: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override suspend fun invoke(uris: Collection<String>): Set<String> {
        val unknown = uris.filterNot { it in knownExisting }
        if (unknown.isNotEmpty()) {
            knownExisting.addAll(delegate(unknown))
        }
        return uris.filterTo(mutableSetOf()) { it in knownExisting }
    }

    override fun invalidateCache() {
        knownExisting.clear()
    }
}
