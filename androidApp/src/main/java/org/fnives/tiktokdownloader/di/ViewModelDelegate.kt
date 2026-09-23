package org.fnives.tiktokdownloader.di

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy

inline fun <reified VM : ViewModel> ComponentActivity.provideViewModels() =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = { ServiceLocator.viewModelFactory() },
        extrasProducer = { defaultViewModelCreationExtras }
    )