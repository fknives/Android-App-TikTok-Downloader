package org.fnives.tiktokdownloader.ui.main

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.ui.main.MainActivity.Companion.INTENT_EXTRA_URL
import org.fnives.tiktokdownloader.ui.shared.Event

class MainViewModel(
    private val processor: VideoDownloadingProcessorUseCase,
    private val addVideoToQueueUseCase: AddVideoToQueueUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _refreshActionVisibility by mutableStateOf(false)
    private var _currentScreen by mutableStateOf<Screen>(Screen.QUEUE)
    val currentScreen get() = _currentScreen
    val refreshActionVisibility by derivedStateOf {
        _refreshActionVisibility && _currentScreen == Screen.QUEUE
    }
    private var _errorMessage by mutableStateOf<Event<ErrorMessage>?>(null)
    val errorMessage by derivedStateOf<Event<ErrorMessage>?> {
        _errorMessage?.takeIf { _currentScreen == Screen.QUEUE }
    }

    init {
        savedStateHandle.get<String>(INTENT_EXTRA_URL)?.let(addVideoToQueueUseCase::invoke)
        savedStateHandle[INTENT_EXTRA_URL] = null
        processor.fetchVideoInState()
        viewModelScope.launch {
            processor.processState.collect {
                val errorMessage = when (it) {
                    is ProcessState.Processing,
                    is ProcessState.Processed,
                    ProcessState.Finished -> null

                    ProcessState.NetworkError -> ErrorMessage.NETWORK
                    ProcessState.ParsingError -> ErrorMessage.PARSING
                    ProcessState.StorageError -> ErrorMessage.STORAGE
                    ProcessState.CaptchaError -> ErrorMessage.CAPTCHA
                    ProcessState.UnknownError -> ErrorMessage.UNKNOWN
                    ProcessState.VideoDeletedError -> ErrorMessage.DELETED
                    ProcessState.VideoPrivateError -> ErrorMessage.PRIVATE
                }
                val refreshActionVisibility = when (it) {
                    is ProcessState.Processing,
                    is ProcessState.Processed,
                    ProcessState.Finished -> false

                    ProcessState.NetworkError,
                    ProcessState.ParsingError,
                    ProcessState.StorageError,
                    ProcessState.UnknownError,
                    ProcessState.VideoDeletedError,
                    ProcessState.VideoPrivateError,
                    ProcessState.CaptchaError -> true
                }
                withContext(Dispatchers.Main) {
                    _errorMessage = errorMessage?.let(::Event)
                    _refreshActionVisibility = refreshActionVisibility
                }
            }
        }
    }

    fun onFetchDownloadClicked() {
        processor.fetchVideoInState()
    }

    fun onScreenSelected(screen: Screen) {
        _currentScreen = screen
    }

    enum class ErrorMessage {
        NETWORK, PARSING, STORAGE, CAPTCHA, UNKNOWN, DELETED, PRIVATE
    }

    enum class Screen {
        QUEUE, SETTINGS, HELP
    }
}