package org.fnives.tiktokdownloader.data.local.verify.exists

interface VerifyFileForUriExists {

    suspend operator fun invoke(uri: String): Boolean
}