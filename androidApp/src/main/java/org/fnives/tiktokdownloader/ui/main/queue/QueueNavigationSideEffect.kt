package org.fnives.tiktokdownloader.ui.main.queue

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.ui.shared.LocalSnackbarHostState

@Composable
fun QueueNavigationSideEffects(viewModel: QueueViewModel) {
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(navigationEvent) {
        val navigationEvent = navigationEvent?.item ?: return@LaunchedEffect
        val intent = when (navigationEvent) {
            is QueueViewModel.NavigationEvent.OpenBrowser -> Intent(
                Intent.ACTION_VIEW,
                navigationEvent.url.toUri()
            )

            is QueueViewModel.NavigationEvent.OpenGallery -> Intent(
                Intent.ACTION_VIEW,
                navigationEvent.uri.toUri()
            )
        }
        try {
            activity?.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val message = activity?.getString(R.string.could_not_open) ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
        }
    }
}