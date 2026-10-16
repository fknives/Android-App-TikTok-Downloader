package org.fnives.tiktokdownloader.data.network.parsing.converter

import org.fnives.tiktokdownloader.data.network.exceptions.CaptchaRequiredException
import org.fnives.tiktokdownloader.data.network.exceptions.HtmlException
import org.fnives.tiktokdownloader.data.network.exceptions.ParsingException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoDeletedException
import org.fnives.tiktokdownloader.data.network.exceptions.VideoPrivateException

abstract class ParsingExceptionThrowingConverter<T> {

    fun doActionSafely(action: () -> T): T {
        try {
            return action()
        } catch (captchaRequiredException: CaptchaRequiredException) {
            throw captchaRequiredException
        } catch (videoDeletedException: VideoDeletedException) {
            throw videoDeletedException
        } catch (videoPrivateException: VideoPrivateException) {
            throw videoPrivateException
        } catch (throwable: Throwable) {
            throw ParsingException(
                cause = throwable,
                html = (throwable as? HtmlException)?.html.orEmpty()
            )
        }
    }

    abstract fun convertSafely(responseBody: String): T
}
