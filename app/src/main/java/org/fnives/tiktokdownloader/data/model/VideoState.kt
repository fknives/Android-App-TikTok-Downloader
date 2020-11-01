package org.fnives.tiktokdownloader.data.model

sealed class VideoState {

    abstract val id: String
    abstract val url: String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    data class InProcess(val videoInProcess: VideoInProgress) : VideoState() {
        override val id: String get() = videoInProcess.id
        override val url: String get() = videoInProcess.url
    }
    data class InPending(val videoInPending: VideoInPending) : VideoState() {
        override val id: String get() = videoInPending.id
        override val url: String get() = videoInPending.url
    }
    data class Downloaded(val videoDownloaded: VideoDownloaded) : VideoState() {
        override val id: String get() = videoDownloaded.id
        override val url: String get() = videoDownloaded.url
    }
}