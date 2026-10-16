package org.fnives.tiktokdownloader.data.local.verify.exists

/**
 * Drops the cached results about which stored videos still have a file on disk.
 */
interface StoredVideosCacheInvalidation {

    fun invalidateCache()
}
