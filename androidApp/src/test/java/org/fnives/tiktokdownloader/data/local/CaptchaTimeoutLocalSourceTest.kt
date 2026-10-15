package org.fnives.tiktokdownloader.data.local

import org.fnives.tiktokdownloader.data.local.persistent.SharedPreferencesManager
import org.fnives.tiktokdownloader.helper.mock.InMemorySharedPreferencesManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.spy

@Suppress("TestFunctionName")
@Timeout(value = 2)
class CaptchaTimeoutLocalSourceTest {

    private lateinit var mockSharedPreferencesManager: SharedPreferencesManager
    private lateinit var sut: CaptchaTimeoutLocalSource

    @BeforeEach
    fun setup() {
        mockSharedPreferencesManager = spy(InMemorySharedPreferencesManager())
        sut = CaptchaTimeoutLocalSource(mockSharedPreferencesManager, 60)
    }

    @Test
    fun GIVEN_initialized_WHEN_isInCacheTimeout_THEN_its_false() {
        Assertions.assertFalse(sut.isInCaptchaTimeout(), "By default not in Captcha timeout")
    }

    @Test
    fun GIVEN_initialized_saved_cache_timeout_WHEN_not_enough_time_passed_THEN_it_is__NOT_InCacheTimeout() {
        sut.onCaptchaResponseReceived()
        Thread.sleep(1)

        Assertions.assertTrue(sut.isInCaptchaTimeout(), "By default not in Captcha timeout")
    }

    @Test
    fun GIVEN_initialized_saved_cache_timeout_WHEN_enough_time_passed_THEN_it_is__InCacheTimeout() {
        sut.onCaptchaResponseReceived()
        Thread.sleep(60)

        Assertions.assertFalse(sut.isInCaptchaTimeout(), "By default not in Captcha timeout")
    }
}