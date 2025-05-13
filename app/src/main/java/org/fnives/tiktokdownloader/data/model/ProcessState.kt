package org.fnives.tiktokdownloader.data.model

sealed class ProcessState {

    data class Processing(val videoInPending: VideoInPending) : ProcessState()
    data class Processed(val videoDownloaded: VideoDownloaded) : ProcessState()
    data object NetworkError : ProcessState()
    data object ParsingError : ProcessState()
    data object VideoDeletedError : ProcessState()
    data object VideoPrivateError : ProcessState()
    data object CaptchaError : ProcessState()
    data object UnknownError : ProcessState()
    data object StorageError : ProcessState()
    data object Finished: ProcessState()
}