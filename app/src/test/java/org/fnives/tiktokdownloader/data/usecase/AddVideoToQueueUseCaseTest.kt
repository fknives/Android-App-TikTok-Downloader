package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.VideoInPendingLocalSource
import org.fnives.tiktokdownloader.data.model.VideoInPending
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@Suppress("TestFunctionName")
@Timeout(value = 2)
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
        verifyNoInteractions(mockUrlVerificationUseCase)
        verifyNoInteractions(mockVideoInPendingLocalSource)
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
        verifyNoInteractions(mockVideoInPendingLocalSource)
    }
}