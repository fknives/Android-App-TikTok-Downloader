package org.fnives.tiktokdownloader.ui.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Single place that decides whether a [Permission] is granted on this device. Provided via DI so
 * both the UI and the activities share the same rule.
 */
class PermissionChecker(private val context: Context) {

    fun isGranted(permission: Permission): Boolean {
        val androidPermission = permission.androidPermission() ?: return true
        return ContextCompat.checkSelfPermission(context, androidPermission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
