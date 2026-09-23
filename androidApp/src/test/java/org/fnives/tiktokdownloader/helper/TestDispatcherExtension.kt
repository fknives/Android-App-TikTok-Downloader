package org.fnives.tiktokdownloader.helper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceUntilIdle() = scheduler.advanceUntilIdle()

@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceTimeBy(delayTimeMillis: Long) = scheduler.advanceTimeBy(delayTimeMillis)