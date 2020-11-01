package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import kotlin.jvm.Throws

class ThrowIfIsCaptchaResponse {

    @Throws(CaptchaRequiredException::class)
    fun invoke(html: String) {
        if (html.isEmpty()) {
            throw CaptchaRequiredException("Empty body")
        } else if (html.contains("captcha.js")) {
            throw CaptchaRequiredException("Contains Captcha keyword")
        }
    }
}