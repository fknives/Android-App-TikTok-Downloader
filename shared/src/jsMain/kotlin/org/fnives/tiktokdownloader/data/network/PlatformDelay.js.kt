@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.fnives.tiktokdownloader.data.network

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

/** The global `setTimeout`; the nodejs stdlib is not on the classpath. */
private external fun setTimeout(handler: () -> Unit, timeout: Int)

internal actual suspend fun platformDelay(duration: Duration) {
    val millis = duration.inWholeMilliseconds
    if (millis <= 0L) return
    suspendCoroutine<Unit> { cont ->
        setTimeout({ cont.resume(Unit) }, millis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
}
