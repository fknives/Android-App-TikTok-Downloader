package org.fnives.tiktokdownloader.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.provideViewModels
import org.fnives.tiktokdownloader.ui.TikTokDownloaderTheme
import org.fnives.tiktokdownloader.ui.main.help.HelpPage
import org.fnives.tiktokdownloader.ui.main.queue.QueuePage
import org.fnives.tiktokdownloader.ui.main.settings.SettingsPage
import org.fnives.tiktokdownloader.ui.service.QueueService
import org.fnives.tiktokdownloader.ui.shared.LocalSnackbarHostState
import org.fnives.tiktokdownloader.ui.shared.NoPaddingSnackbar

class MainActivity : AppCompatActivity() {

    private val viewModel by provideViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stopService(QueueService.buildIntent(this))
        lifecycle.addObserver(viewModel)

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            val resources = LocalResources.current
            LaunchedEffect(viewModel.errorMessage) {
                viewModel.errorMessage?.item?.let {
                    snackbarHostState.showSnackbar(resources.getString(it.stringRes))
                }
            }
            RequestStoragePermissionEffect()
            RequestNotificationPermissionEffectIfNeeded()

            TikTokDownloaderTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    AppScaffold(viewModel)
                }
            }
        }
    }

    companion object {
        const val INTENT_EXTRA_URL = "INTENT_EXTRA_URL"

        fun buildIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)

        fun buildIntent(context: Context, url: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(INTENT_EXTRA_URL, url)

        @get:StringRes
        private val MainViewModel.ErrorMessage.stringRes: Int
            get() = when (this) {
                MainViewModel.ErrorMessage.NETWORK -> R.string.network_error
                MainViewModel.ErrorMessage.PARSING -> R.string.parsing_error
                MainViewModel.ErrorMessage.STORAGE -> R.string.storage_error
                MainViewModel.ErrorMessage.CAPTCHA -> R.string.captcha_error
                MainViewModel.ErrorMessage.UNKNOWN -> R.string.unexpected_error
                MainViewModel.ErrorMessage.DELETED -> R.string.video_deleted_error
                MainViewModel.ErrorMessage.PRIVATE -> R.string.video_private_error
            }
    }
}

@Composable
fun AppScaffold(viewModel: MainViewModel) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    MainViewModel.Screen.QUEUE,
                    MainViewModel.Screen.SETTINGS,
                    MainViewModel.Screen.HELP
                ).forEach { screen ->
                    AppNavigationItem(
                        screen = screen,
                        viewModel.currentScreen,
                        viewModel::onScreenSelected
                    )
                }
            }
        },
        floatingActionButton = {
            FabWithSnackBar(viewModel)
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            Crossfade(
                targetState = viewModel.currentScreen,
                label = "main screen"
            ) { screen ->
                when (screen) {
                    MainViewModel.Screen.QUEUE -> QueuePage()
                    MainViewModel.Screen.SETTINGS -> SettingsPage()
                    MainViewModel.Screen.HELP -> HelpPage()
                }
            }
        }
    }
}

@Composable
private fun FabWithSnackBar(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        AnimatedVisibility(
            viewModel.currentScreen == MainViewModel.Screen.QUEUE,
            modifier = Modifier.weight(1f)
        ) {
            SnackbarHost(
                hostState = LocalSnackbarHostState.current,
                modifier = Modifier.fillMaxWidth(),
                { data ->
                    NoPaddingSnackbar(
                        snackbarData = data,
                        modifier = Modifier.absolutePadding(left = 24.dp),
                    )
                }
            )
        }
        val fabScale by animateFloatAsState(if (viewModel.refreshActionVisibility) 1f else 0f)
        val rotation = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        FloatingActionButton(
            modifier = Modifier.scale(fabScale),
            onClick = {
                viewModel.onFetchDownloadClicked()
                scope.launch {
                    rotation.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = LinearEasing
                        )
                    )
                    rotation.snapTo(0f)
                }
            }
        ) {
            Icon(
                painterResource(R.drawable.ic_round_refresh),
                contentDescription = stringResource(R.string.start_download),
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation.value
                }
            )
        }
    }
}

@Composable
private fun RowScope.AppNavigationItem(
    screen: MainViewModel.Screen,
    current: MainViewModel.Screen,
    onClick: (MainViewModel.Screen) -> Unit
) {
    NavigationBarItem(
        selected = current == screen,
        onClick = { onClick(screen) },
        icon = {
            Icon(
                painter = painterResource(
                    when (screen) {
                        MainViewModel.Screen.QUEUE -> R.drawable.ic_download
                        MainViewModel.Screen.SETTINGS -> R.drawable.ic_twotone_settings
                        MainViewModel.Screen.HELP -> R.drawable.ic_twotone_help
                    }
                ),
                contentDescription = null
            )
        },
        label = {
            Text(
                stringResource(
                    when (screen) {
                        MainViewModel.Screen.QUEUE -> R.string.queue
                        MainViewModel.Screen.SETTINGS -> R.string.settings
                        MainViewModel.Screen.HELP -> R.string.help
                    }
                )
            )
        }
    )
}