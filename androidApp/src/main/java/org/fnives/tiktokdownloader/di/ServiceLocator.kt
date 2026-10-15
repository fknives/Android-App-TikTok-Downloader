package org.fnives.tiktokdownloader.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fnives.tiktokdownloader.di.module.AndroidFileManagementModule
import org.fnives.tiktokdownloader.di.module.LocalSourceModule
import org.fnives.tiktokdownloader.di.module.NetworkModule
import org.fnives.tiktokdownloader.di.module.PermissionModule
import org.fnives.tiktokdownloader.di.module.UseCaseModule
import org.fnives.tiktokdownloader.di.module.ViewModelModule
import org.fnives.tiktokdownloader.ui.permission.PermissionChecker
import org.fnives.tiktokdownloader.ui.service.QueueServiceViewModel
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
object ServiceLocator {

    private val DEFAULT_DELAY_BEFORE_REQUEST = TimeUnit.SECONDS.toMillis(4)
    private var _viewModelModule: ViewModelModule? = null
    private val viewModelModule: ViewModelModule
        get() = _viewModelModule ?: throw IllegalStateException("$this.start has not been called!")

    private var _permissionModule: PermissionModule? = null
    val permissionModule: PermissionModule
        get() = _permissionModule ?: throw IllegalStateException("$this.start has not been called!")

    val permissionChecker: PermissionChecker
        get() = permissionModule.permissionChecker

    private var _useCaseModule: UseCaseModule? = null
    val useCaseModule: UseCaseModule
        get() = _useCaseModule ?: throw IllegalStateException("$this.start has not been called!")

    fun viewModelFactory(): ViewModelProvider.Factory =
        ViewModelFactory(viewModelModule)

    val queueServiceViewModel: QueueServiceViewModel
        get() = viewModelModule.queueServiceViewModel

    fun start(context: Context) {
        val androidFileManagementModule = AndroidFileManagementModule(context)
        val localSourceModule =
            LocalSourceModule(androidFileManagementModule = androidFileManagementModule)
        val networkModule = NetworkModule(delayBeforeRequest = DEFAULT_DELAY_BEFORE_REQUEST)
        val useCaseModule = UseCaseModule(
            localSourceModule = localSourceModule,
            networkModule = networkModule
        )
        _useCaseModule = useCaseModule
        _permissionModule = PermissionModule(context)
        _viewModelModule = ViewModelModule(useCaseModule)
    }

    @Composable
    inline fun <reified T : ViewModel> viewModel() = viewModel<T>(factory = viewModelFactory())
}