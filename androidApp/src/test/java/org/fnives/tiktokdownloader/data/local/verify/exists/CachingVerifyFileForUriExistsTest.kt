package org.fnives.tiktokdownloader.data.local.verify.exists

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@Suppress("TestFunctionName")
@Timeout(value = 2)
class CachingVerifyFileForUriExistsTest {

    private lateinit var mockDelegate: VerifyFileForUriExists
    private lateinit var sut: CachingVerifyFileForUriExists

    @BeforeEach
    fun setup() {
        mockDelegate = mock()
        sut = CachingVerifyFileForUriExists(mockDelegate)
    }

    @Test
    fun GIVEN_empty_uris_WHEN_invoked_THEN_delegate_is_not_queried_and_empty_is_returned() =
        runBlocking {
            val result = sut(emptyList())

            Assertions.assertEquals(emptySet<String>(), result)
            verifyNoInteractions(mockDelegate)
        }

    @Test
    fun GIVEN_unknown_uris_WHEN_invoked_THEN_delegate_is_queried_and_only_existing_are_returned() =
        runBlocking<Unit> {
            whenever(mockDelegate.invoke(listOf("a", "b"))).doReturn(setOf("a"))

            val result = sut(listOf("a", "b"))

            Assertions.assertEquals(setOf("a"), result)
            verify(mockDelegate, times(1)).invoke(listOf("a", "b"))
        }

    @Test
    fun GIVEN_cached_uri_WHEN_invoked_again_THEN_delegate_is_not_queried_again() = runBlocking<Unit> {
        whenever(mockDelegate.invoke(listOf("a"))).doReturn(setOf("a"))

        val first = sut(listOf("a"))
        val second = sut(listOf("a"))

        Assertions.assertEquals(setOf("a"), first)
        Assertions.assertEquals(setOf("a"), second)
        verify(mockDelegate, times(1)).invoke(listOf("a"))
    }

    @Test
    fun GIVEN_partially_cached_uris_WHEN_invoked_THEN_only_the_unknown_are_queried() =
        runBlocking<Unit> {
            whenever(mockDelegate.invoke(listOf("a"))).doReturn(setOf("a"))
            whenever(mockDelegate.invoke(listOf("b"))).doReturn(setOf("b"))

            sut(listOf("a"))
            val result = sut(listOf("a", "b"))

            Assertions.assertEquals(setOf("a", "b"), result)
            verify(mockDelegate, times(1)).invoke(listOf("a"))
            verify(mockDelegate, times(1)).invoke(listOf("b"))
        }

    @Test
    fun GIVEN_non_existing_uri_WHEN_invoked_again_THEN_delegate_is_queried_again() = runBlocking<Unit> {
        whenever(mockDelegate.invoke(listOf("a"))).doReturn(emptySet())

        val first = sut(listOf("a"))
        val second = sut(listOf("a"))

        Assertions.assertEquals(emptySet<String>(), first)
        Assertions.assertEquals(emptySet<String>(), second)
        verify(mockDelegate, times(2)).invoke(listOf("a"))
    }

    @Test
    fun GIVEN_invalidated_cache_WHEN_invoked_THEN_delegate_is_queried_again() = runBlocking<Unit> {
        whenever(mockDelegate.invoke(listOf("a"))).doReturn(setOf("a"))

        sut(listOf("a"))
        sut.invalidateCache()
        val result = sut(listOf("a"))

        Assertions.assertEquals(setOf("a"), result)
        verify(mockDelegate, times(2)).invoke(listOf("a"))
    }
}
