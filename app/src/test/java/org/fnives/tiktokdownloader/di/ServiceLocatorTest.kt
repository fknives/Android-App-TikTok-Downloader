package org.fnives.tiktokdownloader.di

import android.content.Context
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.fnives.tiktokdownloader.helper.mock.MockSavedStateRegistryOwner
import org.fnives.tiktokdownloader.ui.main.MainViewModel
import org.fnives.tiktokdownloader.ui.main.queue.QueueViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ServiceLocatorTest {

    private lateinit var mockContext : Context

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        mockContext = mock()
        whenever(mockContext.applicationContext).doReturn(mockContext)
        whenever(mockContext.contentResolver).doReturn(mock())
        whenever(mockContext.getSharedPreferences(anyOrNull(), anyOrNull())).doReturn(mock())
        ServiceLocator.start(mockContext)
    }

    @AfterEach
    fun tearDown(){
        Dispatchers.resetMain()
    }

    @Test
    fun verifyQueueDownloadViewModelCanBeCreated() {
        ServiceLocator.queueServiceViewModel
    }

    @Test
    fun verifyQueueViewModelCanBeCreated() {
        ServiceLocator.viewModelFactory(MockSavedStateRegistryOwner(), mock()).create(QueueViewModel::class.java)
    }

    @Test
    fun verifyMainViewModelCanBeCreated() {
        ServiceLocator.viewModelFactory(MockSavedStateRegistryOwner(), mock()).create(MainViewModel::class.java)
    }
}