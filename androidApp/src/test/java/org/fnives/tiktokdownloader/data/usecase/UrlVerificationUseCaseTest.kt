package org.fnives.tiktokdownloader.data.usecase

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout


@Timeout(value = 2)
class UrlVerificationUseCaseTest {

    private lateinit var sut: UrlVerificationUseCase

    @BeforeEach
    fun setup() {
        sut = UrlVerificationUseCase()
    }

    @Test
    fun GIVEN_url_containing_tiktok_THEN_it_is_ok() {
        val actual = sut.invoke("https://vm.tiktok.com/d42dfsf")

        Assertions.assertTrue(actual, "Url is considered NOT valid")
    }

    @Test
    fun GIVEN_url_NOT_containing_tiktok_THEN_it_is_ok() {
        val actual = sut.invoke("https://vm.t.com/d42dfsf")

        Assertions.assertFalse(actual, "Url is considered valid")
    }
}