package org.fnives.tiktokdownloader.data.network

import kotlin.time.Duration

internal expect suspend fun platformDelay(duration: Duration)
