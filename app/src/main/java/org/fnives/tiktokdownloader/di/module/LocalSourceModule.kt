package org.fnives.tiktokdownloader.di.module

import org.fnives.tiktokdownloader.data.local.CaptchaTimeoutLocalSource
import org.fnives.tiktokdownloader.data.local.VideoDownloadedLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.local.VideoInProgressLocalSource
import java.util.concurrent.TimeUnit

class LocalSourceModule(private val androidFileManagementModule: AndroidFileManagementModule) {

    val videoDownloadedLocalSource: VideoDownloadedLocalSource
        get() = VideoDownloadedLocalSource(
            saveVideoFile = androidFileManagementModule.saveVideoFile,
            sharedPreferencesManagerImpl = androidFileManagementModule.sharedPreferencesManager,
            verifyFileForUriExists = androidFileManagementModule.verifyFileForUriExists
        )

    val videoInPendingLocalSource: VideoInPendingLocalSource
        get() = VideoInPendingLocalSource(
            sharedPreferencesManager = androidFileManagementModule.sharedPreferencesManager
        )

    val videoInProgressLocalSource: VideoInProgressLocalSource by lazy { VideoInProgressLocalSource() }

    val captchaTimeoutLocalSource: CaptchaTimeoutLocalSource
        get() = CaptchaTimeoutLocalSource(
            androidFileManagementModule.sharedPreferencesManager,
            DEFAULT_CAPTCHA_TIMEOUT
        )

    companion object {
        private val DEFAULT_CAPTCHA_TIMEOUT = TimeUnit.MINUTES.toMillis(10)
    }
}