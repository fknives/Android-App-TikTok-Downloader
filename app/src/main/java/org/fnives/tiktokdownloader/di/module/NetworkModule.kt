package org.fnives.tiktokdownloader.di.module

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.fnives.tiktokdownloader.BuildConfig
import org.fnives.tiktokdownloader.data.network.TikTokDownloadRemoteSource
import org.fnives.tiktokdownloader.data.network.TikTokRetrofitService
import org.fnives.tiktokdownloader.data.network.parsing.TikTokWebPageConverterFactory
import org.fnives.tiktokdownloader.data.network.parsing.converter.ThrowIfIsCaptchaResponse
import org.fnives.tiktokdownloader.data.network.session.CookieSavingInterceptor
import org.fnives.tiktokdownloader.data.network.session.CookieStore
import retrofit2.Converter
import retrofit2.Retrofit

class NetworkModule(private val delayBeforeRequest: Long) {

    private val throwIfIsCaptchaResponse: ThrowIfIsCaptchaResponse
        get() = ThrowIfIsCaptchaResponse()

    private val tikTokConverterFactory: Converter.Factory
        get() = TikTokWebPageConverterFactory(throwIfIsCaptchaResponse)

    private val cookieSavingInterceptor: CookieSavingInterceptor by lazy { CookieSavingInterceptor() }

    private val cookieStore: CookieStore get() = cookieSavingInterceptor

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(cookieSavingInterceptor)
            .let {
                if (BuildConfig.DEBUG) {
                    it.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                } else {
                    it
                }
            }
            .build()

    private val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl("https://google.com")
            .addConverterFactory(tikTokConverterFactory)
            .client(okHttpClient)
            .build()

    private val tikTokRetrofitService: TikTokRetrofitService
        get() = retrofit.create(TikTokRetrofitService::class.java)

    val tikTokDownloadRemoteSource: TikTokDownloadRemoteSource
        get() = TikTokDownloadRemoteSource(delayBeforeRequest, tikTokRetrofitService, cookieStore)
}