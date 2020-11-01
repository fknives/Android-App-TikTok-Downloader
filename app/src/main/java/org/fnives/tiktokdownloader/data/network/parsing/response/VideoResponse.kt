package org.fnives.tiktokdownloader.data.network.parsing.response

import okhttp3.MediaType
import java.io.InputStream

class VideoResponse(val mediaType: MediaType?, val videoInputStream: InputStream)