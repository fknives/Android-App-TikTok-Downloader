package org.fnives.tiktokdownloader.data.local.persistent

import kotlinx.coroutines.flow.Flow

interface SharedPreferencesManager {
    var captchaTimeoutUntil: Long
    var pendingVideos: Set<String>
    val pendingVideosFlow: Flow<Set<String>>
    var downloadedVideos: Set<String>
    val downloadedVideosFlow: Flow<Set<String>>
}