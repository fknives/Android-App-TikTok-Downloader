package org.fnives.tiktokdownloader.data.network

import kotlinx.coroutines.delay
import kotlin.time.Duration

internal actual suspend fun platformDelay(duration: Duration) {
    delay(duration)
}
