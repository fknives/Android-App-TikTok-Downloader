package org.fnives.tiktokdownloader.helper.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager

class InMemorySharedPreferencesManager : SharedPreferencesManager {

    private val _pendingVideosFlow = MutableStateFlow<Set<String>>(emptySet())
    override val pendingVideosFlow: Flow<Set<String>> = _pendingVideosFlow
    override var captchaTimeoutUntil: Long = 0
    override var pendingVideos: Set<String>
        get() = _pendingVideosFlow.value
        set(value) {
            _pendingVideosFlow.value = value
        }
    private val _downloadedVideosFlow = MutableStateFlow<Set<String>>(emptySet())
    override val downloadedVideosFlow: Flow<Set<String>> = _downloadedVideosFlow
    override var downloadedVideos: Set<String>
        get() = _downloadedVideosFlow.value
        set(value) {
            _downloadedVideosFlow.value = value
        }
}