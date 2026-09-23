package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.fnives.tiktokdownloader.data.model.VideoInProgress
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout


@Timeout(value = 2)
class VideoInProgressLocalSourceTest {

    private lateinit var sut: VideoInProgressLocalSource

    @BeforeEach
    fun setup() {
        sut = VideoInProgressLocalSource()
    }

    // region VIDEO IN PROGRESS
    @Test
    fun GIVEN_observing_videoInProcessFlow_WHEN_initialization_THEN_it_returns_null() =
        runBlocking<Unit> {
            Assertions.assertEquals(null, sut.videoInProcessFlow.first())
        }

    @Test
    fun GIVEN_observing_videoInProcessFlow_WAITING_for_2_items_WHEN_initialization_THEN_it_timesOut() {
        Assertions.assertThrows(TimeoutCancellationException::class.java) {
            runBlocking<Unit> {
                withTimeout(300) {
                    sut.videoInProcessFlow.take(2).toList()
                }
            }
        }
    }

    @Test
    fun GIVEN_observing_from_videoInProcessFlow_WHEN_markedVideoAsInProgress_THEN_it_is_returned() =
        runBlocking<Unit> {
            val actual = async(coroutineContext) {
                sut.videoInProcessFlow.take(2).toList()
            }
            yield()
            sut.markVideoAsInProgress(VideoInPending("id", "alma"))

            Assertions.assertEquals(VideoInProgress("id", "alma"), sut.videoInProcessFlow.first())
            Assertions.assertEquals(listOf(null, VideoInProgress("id", "alma")), actual.await())
        }

    @Test
    fun GIVEN_observing_videoInProcessFlow_AND_markedVideoAsInProcess_WHEN_unmarking_THEN_null_is_returned() =
        runBlocking {
            sut.markVideoAsInProgress(VideoInPending("id", "alma"))

            val actual = async(coroutineContext) {
                sut.videoInProcessFlow.take(2).toList()
            }
            yield()
            sut.removeVideoAsInProgress(VideoInPending("id", "alma"))

            Assertions.assertEquals(null, sut.videoInProcessFlow.first())
            Assertions.assertEquals(listOf(VideoInProgress("id", "alma"), null), actual.await())
        }

    @Test
    fun GIVEN_observing_videoInProcessFlow_AND_markedVideoAsInProcess_WHEN_unmarking_different_one_THEN_nothing_is_sent_out() =
        runBlocking {
            sut.markVideoAsInProgress(VideoInPending("id", "alma"))

            val actual = async(coroutineContext) {
                sut.videoInProcessFlow.take(1).toList()
            }
            yield()
            sut.removeVideoAsInProgress(VideoInPending("-----id------", "alma"))

            Assertions.assertEquals(VideoInProgress("id", "alma"), sut.videoInProcessFlow.first())
            Assertions.assertEquals(listOf(VideoInProgress("id", "alma")), actual.await())
        }
    // endregion
}