package org.fnives.tiktokdownloader.data.model

sealed class ProcessState {

    data class Processing(val videoInPending: VideoInPending) : ProcessState()
    data class Processed(val videoDownloaded: VideoDownloaded) : ProcessState()
    object NetworkError : ProcessState()
    object ParsingError : ProcessState()
    object CaptchaError : ProcessState()
    object UnknownError : ProcessState()
    object StorageError : ProcessState()
    object Finished: ProcessState()
}