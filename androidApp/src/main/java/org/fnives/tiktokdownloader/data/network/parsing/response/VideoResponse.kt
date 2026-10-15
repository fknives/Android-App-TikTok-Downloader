package org.fnives.tiktokdownloader.data.network.parsing.response

import androidx.annotation.Keep
import okhttp3.MediaType
import java.io.InputStream

@Keep
class VideoResponse(val mediaType: MediaType?, val videoInputStream: InputStream)