package org.fnives.tiktokdownloader.data.local

import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager

class CaptchaTimeoutLocalSource(
    private val appSharedPreferencesManager: SharedPreferencesManager,
    private val timeOutInMillis : Long
) {

    fun isInCaptchaTimeout(): Boolean =
        System.currentTimeMillis() < appSharedPreferencesManager.captchaTimeoutUntil

    fun onCaptchaResponseReceived() {
        appSharedPreferencesManager.captchaTimeoutUntil = System.currentTimeMillis() + timeOutInMillis
    }
}