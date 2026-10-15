package org.fnives.tiktokdownloader.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import org.fnives.tiktokdownloader.di.module.ViewModelModule
import org.fnives.tiktokdownloader.ui.main.MainViewModel
import org.fnives.tiktokdownloader.ui.main.queue.QueueViewModel
import org.fnives.tiktokdownloader.ui.main.settings.SettingsViewModel

class ViewModelFactory(
    private val viewModelModule: ViewModelModule,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when (modelClass) {
            QueueViewModel::class.java -> viewModelModule.queueViewModel
            SettingsViewModel::class.java -> viewModelModule.settignsViewModel
            else -> throw IllegalArgumentException("Can't create viewModel for $modelClass ")
        }
        return viewModel as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel = when (modelClass) {
            MainViewModel::class.java -> viewModelModule.mainViewModel(extras.createSavedStateHandle())
            else -> create(modelClass)
        }
        return viewModel as T
    }
}