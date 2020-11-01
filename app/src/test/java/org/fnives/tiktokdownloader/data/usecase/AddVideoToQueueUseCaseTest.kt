package org.fnives.tiktokdownloader.data.usecase

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("TestFunctionName")
class AddVideoToQueueUseCaseTest {

    private lateinit var sut: AddVideoToQueueUseCase
    private lateinit var mockUrlVerificationUseCase: UrlVerificationUseCase
    private lateinit var mockVideoInPendingLocalSource: VideoInPendingLocalSource

    @BeforeEach
    fun setup() {
        mockUrlVerificationUseCase = mock()
        mockVideoInPendingLocalSource = mock()
        sut = AddVideoToQueueUseCase(mockUrlVerificationUseCase, mockVideoInPendingLocalSource)
    }

    @Test
    fun GIVEN_no_action_THEN_the_local_source_and_verifier_is_not_touched() {
        verifyZeroInteractions(mockUrlVerificationUseCase)
        verifyZeroInteractions(mockVideoInPendingLocalSource)
    }

    @Test
    fun GIVEN_url_WHEN_calling_add_to_queue_THEN_it_is_delegated_to_storage() {
        val expectedUrl = "https://unquie.url"
        lateinit var argument : VideoInPending
        whenever(mockUrlVerificationUseCase.invoke(anyOrNull())).doReturn(true)
        whenever(mockVideoInPendingLocalSource.saveUrlIntoQueue(anyOrNull())).then {
            argument = it.arguments.first() as VideoInPending
            Unit
        }

        val actual = sut.invoke(expectedUrl)

        Assertions.assertTrue(actual, "Url is NOT Saved while it should be")
        verify(mockUrlVerificationUseCase, times(1)).invoke(expectedUrl)
        verifyNoMoreInteractions(mockUrlVerificationUseCase)
        verify(mockVideoInPendingLocalSource, times(1)).saveUrlIntoQueue(argument)
        verifyNoMoreInteractions(mockVideoInPendingLocalSource)
        Assertions.assertEquals(VideoInPending(argument.id, url = expectedUrl), argument)
        Assertions.assertTrue(argument.id.isNotBlank(), "Created VideoInPending Id is BLANK")
    }

    @Test
    fun GIVEN_throwing_saveURl_WHEN_calling_add_to_queue_THEN_it_throws_the_same_exception() {
        Assertions.assertThrows(Throwable::class.java) {
            val expectedException = Throwable()
            whenever(mockUrlVerificationUseCase.invoke(anyOrNull())).doReturn(true)
            whenever(mockVideoInPendingLocalSource.saveUrlIntoQueue(anyOrNull())).then {
                throw expectedException
            }

            try {
                sut.invoke("alma")
            } catch (throwable: Throwable) {
                Assertions.assertSame(expectedException, throwable)
                throw throwable
            }
        }
    }

    @Test
    fun GIVEN_url_WHEN_verification_fails_THEN_it_is_not_saved_into_localSource() {
        val expectedUrl = "https://unquie.url"
        whenever(mockUrlVerificationUseCase.invoke(anyOrNull())).doReturn(false)

        val actual = sut.invoke(expectedUrl)

        Assertions.assertFalse(actual, "Url is Saved while it should NOT be")
        verify(mockUrlVerificationUseCase, times(1)).invoke(expectedUrl)
        verifyNoMoreInteractions(mockUrlVerificationUseCase)
        verifyZeroInteractions(mockVideoInPendingLocalSource)
    }
}