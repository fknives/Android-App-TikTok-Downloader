package org.fnives.tiktokdownloader.data.network

import org.fnives.tiktokdownloader.data.network.parsing.response.ActualVideoPageUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoFileUrl
import org.fnives.tiktokdownloader.data.network.parsing.response.VideoResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface TikTokRetrofitService {

    @GET
    @Headers(
        "Origin: https://www.tiktok.com",
        "Referer: https://www.tiktok.com/",
        "Sec-Fetch-Dest: empty",
        "Sec-Fetch-Mode: cors",
        "Sec-Fetch-Site: cross-site",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36"
    )
    suspend fun getContentActualUrlAndCookie(@Url tiktokLink: String): ActualVideoPageUrl

    @GET
    @Headers(
        "Origin: https://www.tiktok.com",
        "Referer: https://www.tiktok.com/",
        "Sec-Fetch-Dest: empty",
        "Sec-Fetch-Mode: cors",
        "Sec-Fetch-Site: cross-site",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36"
    )
    suspend fun getVideoUrl(@Url tiktokLink: String): VideoFileUrl

    @Headers(
        "Referer: https://www.tiktok.com/",
        "Sec-Fetch-Dest: video",
        "Sec-Fetch-Mode: no-cors",
        "Sec-Fetch-Site: cross-site",
        "Accept: */*",
        "Accept-Encoding: identity;q=1, *;q=0",
        "Accept-Language: en-US,en;q=0.9,hu-HU;q=0.8,hu;q=0.7,ro;q=0.6",
        "Connection: keep-alive",
        "Range: bytes=0-",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36"
    )
    @GET
    suspend fun getVideo(@Url videoUrl: String): VideoResponse
}