package org.fnives.tiktokdownloader.data.usecase

class UrlVerificationUseCase {

    operator fun invoke(url: String) = url.contains("tiktok")
}