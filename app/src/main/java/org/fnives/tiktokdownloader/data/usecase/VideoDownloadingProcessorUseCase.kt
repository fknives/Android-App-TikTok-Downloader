package org.fnives.tiktokdownloader.data.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.fnives.tiktokdownloader.data.local.CaptchaTimeoutLocalSource
import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource
import org.fnives.tiktokdownloader.data.local.exceptions.StorageException
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.model.VideoDownloaded
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInSavingIntoFile
import org.fnives.tiktokdownloader.data.network.TikTokDownloadRemoteSource
import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.NetworkException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException

@OptIn(FlowPreview::class)
class VideoDownloadingProcessorUseCase(
    private val tikTokDownloadRemoteSource: TikTokDownloadRemoteSource,
    private val videoInProgressLocalSource: VideoInProgressLocalSource,
    private val videoInPendingLocalSource: VideoInPendingLocalSource,
    private val videoDownloadedLocalSource: VideoDownloadedLocalSource,
    private val captchaTimeoutLocalSource: CaptchaTimeoutLocalSource,
    dispatcher: CoroutineDispatcher
) {

    private val fetch = MutableStateFlow(ProcessingState.RUNNING)
    private val _processState by lazy {
        combineIntoPair(fetch, videoInPendingLocalSource.observeLastPendingVideo())
            .filter { it.first == ProcessingState.RUNNING }
            .map { it.second }
            .debounce(WORK_FLOW_DEBOUNCE)
            .flatMapConcat(::finishedOrProcessItem)
            .distinctUntilChanged()
            .onEach {
                if (it.isError()) {
                    fetch.value = ProcessingState.ERROR
                }
            }
            .shareIn(CoroutineScope(dispatcher), SharingStarted.Lazily)
    }
    val processState: Flow<ProcessState> get() = _processState

    fun fetchVideoInState() {
        fetch.value = ProcessingState.RUNNING
    }

    private fun finishedOrProcessItem(videoInPending: VideoInPending?): Flow<ProcessState> =
        if (videoInPending == null) {
            flowOf(ProcessState.Finished)
        } else {
            processItemFlow(videoInPending)
        }

    private fun processItemFlow(videoInPending: VideoInPending): Flow<ProcessState> =
        flow {
            emit(ProcessState.Processing(videoInPending))
            emit(downloadVideo(videoInPending))
        }

    private suspend fun downloadVideo(videoInPending: VideoInPending): ProcessState =
        try {
            val alreadyDownloaded = videoDownloadedLocalSource.savedVideos.first()
                .firstOrNull { it.id == videoInPending.id }
            val videoDownloaded = when {
                alreadyDownloaded != null -> {
                    videoInPendingLocalSource.removeVideoFromQueue(videoInPending)
                    alreadyDownloaded
                }
                captchaTimeoutLocalSource.isInCaptchaTimeout() -> {
                    throw CaptchaRequiredException("In Captcha Timeout!")
                }
                else -> {
                    videoInProgressLocalSource.markVideoAsInProgress(videoInPending)
                    val videoInSavingIntoFile: VideoInSavingIntoFile = tikTokDownloadRemoteSource.getVideo(videoInPending)
                    val videoDownloaded: VideoDownloaded = videoDownloadedLocalSource.saveVideo(videoInSavingIntoFile)
                    videoInPendingLocalSource.removeVideoFromQueue(videoInPending)

                    videoDownloaded
                }
            }

            ProcessState.Processed(videoDownloaded)
        } catch (networkException: NetworkException) {
            ProcessState.NetworkError
        } catch (parsingException: ParsingException) {
            ProcessState.ParsingError
        } catch (storageException: StorageException) {
            ProcessState.StorageError
        } catch (captchaRequiredException: CaptchaRequiredException) {
            captchaTimeoutLocalSource.onCaptchaResponseReceived()
            ProcessState.CaptchaError
        } catch (throwable: Throwable) {
            ProcessState.UnknownError
        } finally {
            videoInProgressLocalSource.removeVideoAsInProgress(videoInPending)
        }

    private enum class ProcessingState {
        RUNNING, ERROR
    }

    companion object {
        private const val WORK_FLOW_DEBOUNCE = 200L

        private fun ProcessState.isError() = when (this) {
            is ProcessState.Processing,
            is ProcessState.Processed,
            ProcessState.Finished -> false
            ProcessState.NetworkError,
            ProcessState.ParsingError,
            ProcessState.StorageError,
            ProcessState.UnknownError,
            ProcessState.CaptchaError -> true
        }

        private fun <T, R> combineIntoPair(flow1: Flow<T>, flow2: Flow<R>): Flow<Pair<T, R>> =
            combine(flow1, flow2) { item1, item2 -> item1 to item2 }

        private fun VideoInPendingLocalSource.observeLastPendingVideo(): Flow<VideoInPending?> =
            pendingVideos.map { it.lastOrNull() }.distinctUntilChanged()
    }
}