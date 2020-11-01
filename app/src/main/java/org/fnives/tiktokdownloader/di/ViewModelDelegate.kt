package org.fnives.tiktokdownloader.di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy

inline fun <reified VM : ViewModel> ComponentActivity.provideViewModels() =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = { createViewModelFactory() }
    )

inline fun <reified VM : ViewModel> Fragment.provideViewModels() =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = { createViewModelFactory() })

fun ComponentActivity.createViewModelFactory() =
    ServiceLocator.viewModelFactory(this, intent?.extras ?: Bundle.EMPTY)

fun Fragment.createViewModelFactory() =
    ServiceLocator.viewModelFactory(this, arguments ?: Bundle.EMPTY)