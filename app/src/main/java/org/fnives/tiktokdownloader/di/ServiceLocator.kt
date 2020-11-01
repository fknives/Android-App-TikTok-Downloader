package org.fnives.tiktokdownloader.di

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import org.fnives.tiktokdownloader.di.module.AndroidFileManagementModule
import org.fnives.tiktokdownloader.di.module.LocalSourceModule
import org.fnives.tiktokdownloader.di.module.NetworkModule
import org.fnives.tiktokdownloader.di.module.PermissionModule
import org.fnives.tiktokdownloader.di.module.UseCaseModule
import org.fnives.tiktokdownloader.di.module.ViewModelModule
import org.fnives.tiktokdownloader.ui.service.QueueServiceViewModel
import java.util.concurrent.TimeUnit

@Suppress("ObjectPropertyName", "MemberVisibilityCanBePrivate")
object ServiceLocator {

    private val DEFAULT_DELAY_BEFORE_REQUEST = TimeUnit.SECONDS.toMillis(4)
    private var _viewModelModule: ViewModelModule? = null
    private val viewModelModule: ViewModelModule
        get() = _viewModelModule ?: throw IllegalStateException("$this.start has not been called!")

    private var _permissionModule: PermissionModule? = null
    val permissionModule: PermissionModule
        get() = _permissionModule ?: throw IllegalStateException("$this.start has not been called!")

    fun viewModelFactory(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        defaultArgs: Bundle
    ): ViewModelProvider.Factory =
        ViewModelFactory(savedStateRegistryOwner, defaultArgs, viewModelModule)

    val queueServiceViewModel: QueueServiceViewModel
        get() = viewModelModule.queueServiceViewModel

    fun start(context: Context) {
        val androidFileManagementModule = AndroidFileManagementModule(context)
        val localSourceModule = LocalSourceModule(androidFileManagementModule)
        val networkModule = NetworkModule(DEFAULT_DELAY_BEFORE_REQUEST)
        val useCaseModule = UseCaseModule(localSourceModule, networkModule)
        _permissionModule = PermissionModule()
        _viewModelModule = ViewModelModule(useCaseModule)
    }
}