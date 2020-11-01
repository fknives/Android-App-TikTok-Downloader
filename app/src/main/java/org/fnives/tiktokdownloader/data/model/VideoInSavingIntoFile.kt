package org.fnives.tiktokdownloader.data.model

import java.io.InputStream

class VideoInSavingIntoFile(
    val id: String,
    val url: String,
    val contentType: ContentType?,
    val byteStream: InputStream
) {
    data class ContentType(val type: String, val subType: String) {

        override fun toString(): String = "$type/$subType"
    }
}