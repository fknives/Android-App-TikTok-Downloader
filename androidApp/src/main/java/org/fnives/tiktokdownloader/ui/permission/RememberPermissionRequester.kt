package org.fnives.tiktokdownloader.ui.permission

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.ServiceLocator

/**
 * Builds a [PermissionRequester] backed by the activity result registry, so any composable can request a [Permission].
 * Grant checks are delegated to the DI [PermissionChecker].
 * The explanation dialog on a denied-but-askable permission is re-triggered through state handled by an effect.
 */
@Composable
fun rememberPermissionRequester(): PermissionRequester {
    val context = LocalContext.current
    val permissionChecker = ServiceLocator.permissionChecker
    val rationaleDialogFactory = remember { PermissionRationaleDialogFactory() }

    var pendingPermission by remember { mutableStateOf<Permission?>(null) }
    var permissionToExplain by remember { mutableStateOf<Permission?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = pendingPermission
        pendingPermission = null
        if (permission != null && !granted) {
            if (permission.shouldShowRationale(context)) {
                permissionToExplain = permission
            } else {
                permission.onPermissionDenied(context)
            }
        }
    }

    val requester = remember(permissionChecker, rationaleDialogFactory, requestPermissionLauncher) {
        object : PermissionRequester {

            override fun isGranted(permission: Permission): Boolean =
                permissionChecker.isGranted(permission)

            override fun request(permission: Permission) {
                val androidPermission = permission.androidPermission() ?: return
                if (isGranted(permission)) {
                    return
                }

                pendingPermission = permission
                if (permission.alwaysShowRationale || permission.shouldShowRationale(context)) {
                    rationaleDialogFactory.show(
                        context = context,
                        title = permission.rationaleTitle,
                        message = permission.rationaleMessage,
                        onOkClicked = { requestPermissionLauncher.launch(androidPermission) },
                        onCanceled = {
                            pendingPermission = null
                            permission.onPermissionDenied(context)
                        }
                    )
                } else {
                    requestPermissionLauncher.launch(androidPermission)
                }
            }
        }
    }

    LaunchedEffect(permissionToExplain) {
        val permission = permissionToExplain ?: return@LaunchedEffect
        permissionToExplain = null
        requester.request(permission)
    }

    return requester
}

private fun Permission.shouldShowRationale(context: Context): Boolean {
    val androidPermission = androidPermission() ?: return false
    return (context as? Activity)?.shouldShowRequestPermissionRationale(androidPermission) == true
}

private fun Permission.onPermissionDenied(context: Context) {
    if (!isMandatory) {
        return
    }
    Toast.makeText(context, R.string.cant_operate_without_permission, Toast.LENGTH_LONG).show()
    (context as? Activity)?.finishAffinity()
}
