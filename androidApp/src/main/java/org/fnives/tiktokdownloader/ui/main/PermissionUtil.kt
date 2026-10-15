package org.fnives.tiktokdownloader.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.ui.permission.Permission
import org.fnives.tiktokdownloader.ui.permission.PermissionRequester
import org.fnives.tiktokdownloader.ui.permission.rememberPermissionRequester

@Composable
fun RequestStoragePermissionEffect() = RequestPermissionEffect {
    request(Permission.STORAGE)
}

@Composable
fun RequestNotificationPermissionEffectIfNeeded() = RequestPermissionEffect {
    if (!ServiceLocator.useCaseModule.getUserPreferences().alwaysOpenApp) {
        request(Permission.NOTIFICATION)
    }
}

@Composable
private fun RequestPermissionEffect(action: suspend PermissionRequester.() -> Unit) {
    val permissionRequester = rememberPermissionRequester()
    var initialPermissionsChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialPermissionsChecked) {
            initialPermissionsChecked = true
            permissionRequester.action()
        }
    }
}