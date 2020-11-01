package org.fnives.tiktokdownloader.data.network.session

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class CookieSavingInterceptor : Interceptor, CookieStore {

    override var cookie: String? = null

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val savedCookies = cookie
        val request = if (savedCookies == null) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Cookie", savedCookies).build()
        }

        val response = chain.proceed(request)
        parseCookie(response)?.let {
            this.cookie = it
        }
        return response
    }

    override fun clear() {
        cookie = null
    }

    companion object {
        private fun parseCookie(response: Response): String? {
            val allCookiesToBeSet = response.headers.toMultimap()["Set-Cookie"] ?: return null
            val cookieWithoutExtraData = allCookiesToBeSet
                .map { cookieWithExtra -> cookieWithExtra.takeWhile { it != ';' } }
                .toSet()

            return cookieWithoutExtraData.joinToString("; ")
        }
    }
}