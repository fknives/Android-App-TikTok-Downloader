package org.fnives.tiktokdownloader.di

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import org.fnives.tiktokdownloader.di.module.ViewModelModule
import org.fnives.tiktokdownloader.ui.main.MainViewModel
import org.fnives.tiktokdownloader.ui.main.queue.QueueViewModel

class ViewModelFactory(
    savedStateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: Bundle,
    private val viewModelModule: ViewModelModule,
) : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, defaultArgs) {

    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val viewModel = when (modelClass) {
            MainViewModel::class.java -> viewModelModule.mainViewModel(handle)
            QueueViewModel::class.java -> viewModelModule.queueViewModel
            else -> throw IllegalArgumentException("Can't create viewModel for $modelClass ")
        }
        return viewModel as T
    }
}