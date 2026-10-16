package org.fnives.tiktokdownloader.data.local.verify.exists

interface VerifyFileForUriExists {

    suspend operator fun invoke(uris: Collection<String>): Set<String>
}