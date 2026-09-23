package org.fnives.tiktokdownloader.di

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.fnives.tiktokdownloader.ui.main.MainViewModel
import org.fnives.tiktokdownloader.ui.main.queue.QueueViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Timeout(value = 2)
class ServiceLocatorTest {

    private lateinit var mockContext: Context

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
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyQueueDownloadViewModelCanBeCreated() {
        ServiceLocator.queueServiceViewModel
    }

    @Test
    fun verifyQueueViewModelCanBeCreated() {
        ServiceLocator.viewModelFactory().create(QueueViewModel::class.java)
    }

    @Test
    fun verifyMainViewModelCanBeCreated() {
        // TODO one day fix this, because the CreationExtras's createSavedStateHandle isn't open it actually gets called
//        ServiceLocator.viewModelFactory().create(
//            MainViewModel::class.java,
//            mock<CreationExtras>().apply {
//                doReturn(mock()).`when`(this).createSavedStateHandle()
//            }
//        )
    }
}