package org.fnives.tiktokdownloader.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.data.model.ProcessState
import org.fnives.tiktokdownloader.data.usecase.AddVideoToQueueUseCase
import org.fnives.tiktokdownloader.data.usecase.VideoDownloadingProcessorUseCase
import org.fnives.tiktokdownloader.ui.main.MainActivity.Companion.INTENT_EXTRA_URL
import org.fnives.tiktokdownloader.ui.shared.Event
import org.fnives.tiktokdownloader.ui.shared.combineNullable

class MainViewModel(
    private val processor: VideoDownloadingProcessorUseCase,
    private val addVideoToQueueUseCase: AddVideoToQueueUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _refreshActionVisibility = MutableLiveData<Boolean>()
    private val currentScreen = MutableLiveData<Screen>()
    val refreshActionVisibility: LiveData<Boolean?> = combineNullable(_refreshActionVisibility, currentScreen) { refreshVisibility, screen ->
        refreshVisibility == true && screen == Screen.QUEUE
    }
    private val _errorMessage = MutableLiveData<Event<ErrorMessage>>()
    val errorMessage: LiveData<Event<ErrorMessage>?> = combineNullable(_errorMessage, currentScreen) { event, screen ->
        event?.takeIf { screen == Screen.QUEUE }
    }

    init {
        savedStateHandle.get<String>(INTENT_EXTRA_URL)?.let(addVideoToQueueUseCase::invoke)
        savedStateHandle.set(INTENT_EXTRA_URL, null)
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
                }
                val refreshActionVisibility = when (it) {
                    is ProcessState.Processing,
                    is ProcessState.Processed,
                    ProcessState.Finished -> false
                    ProcessState.NetworkError,
                    ProcessState.ParsingError,
                    ProcessState.StorageError,
                    ProcessState.UnknownError,
                    ProcessState.CaptchaError -> true
                }
                _errorMessage.postValue(errorMessage?.let(::Event))
                _refreshActionVisibility.postValue(refreshActionVisibility)
            }
        }
    }

    fun onFetchDownloadClicked() {
        processor.fetchVideoInState()
    }

    fun onScreenSelected(screen: Screen) {
        currentScreen.value = screen
    }

    enum class ErrorMessage {
        NETWORK, PARSING, STORAGE, CAPTCHA, UNKNOWN
    }

    enum class Screen {
        QUEUE, HELP
    }
}