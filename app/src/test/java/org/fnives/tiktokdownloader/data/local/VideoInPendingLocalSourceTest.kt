package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.fnives.tiktokdownloader.helper.mock.InMemorySharedPreferencesManager
import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout


@Timeout(value = 2)
class VideoInPendingLocalSourceTest {

    private lateinit var sut: VideoInPendingLocalSource
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    @BeforeEach
    fun setup() {
        sharedPreferencesManager = InMemorySharedPreferencesManager()
        sut = VideoInPendingLocalSource(sharedPreferencesManager)
    }

    @Test
    fun GIVEN_observing_PendingVideos_WHEN_initialized_THEN_EmptySet_is_sent_out() = runBlocking {
        Assertions.assertEquals(emptyList<VideoInPending>(), sut.pendingVideos.first())
    }

    @Test
    fun GIVEN_observing_PendingVideos_for_2_values_WHEN_initialized_THEN_it_times_out() {
        Assertions.assertThrows(TimeoutCancellationException::class.java) {
            runBlocking<Unit> {
                withTimeout(300) {
                    sut.pendingVideos.take(2).toList()
                }
            }
        }
    }

    @Test
    fun GIVEN_observing_PendingVideos_WHEN_marking_a_video_pending_THEN_it_is_sent_out() = runBlocking<Unit> {
        runBlocking {
            val actual = async(coroutineContext) {
                sut.pendingVideos.take(2).toList()
            }
            yield()
            sut.saveUrlIntoQueue(VideoInPending("id", "alma"))

            Assertions.assertEquals(listOf(VideoInPending("id", "alma")), sut.pendingVideos.first())
            Assertions.assertEquals(listOf(emptyList(), listOf(VideoInPending("id", "alma"))), actual.await())
        }
    }

    @Test
    fun GIVEN_observing_PendingVideos_AND_video_marked_as_pending_WHEN_saving_video_THEN_its_no_longer_sent_out_as_pending() =
        runBlocking<Unit> {
            val videoInPending = VideoInPending("id", "alma")
            sut.saveUrlIntoQueue(videoInPending)

            val actual = async(coroutineContext) {
                sut.pendingVideos.take(2).toList()
            }
            yield()
            sut.removeVideoFromQueue(videoInPending)

            Assertions.assertEquals(listOf(listOf(VideoInPending("id", "alma")), emptyList()), actual.await())
        }

    @Test
    fun GIVEN_observing_PendingVideos_WHEN_2_video_marked_as_pending_THEN_both_of_them_are_sent_out_in_correct_order() =
        runBlocking<Unit> {
            val videoInPending1 = VideoInPending("id1", "alma1")
            val videoInPending2 = VideoInPending("id2", "alma2")
            val expected = listOf(emptyList(), listOf(videoInPending1), listOf(videoInPending1, videoInPending2))

            val actual = async(coroutineContext) {
                sut.pendingVideos.take(3).toList()
            }
            yield()
            sut.saveUrlIntoQueue(videoInPending1)
            delay(10)
            yield()
            sut.saveUrlIntoQueue(videoInPending2)

            Assertions.assertEquals(expected, actual.await())
        }

}