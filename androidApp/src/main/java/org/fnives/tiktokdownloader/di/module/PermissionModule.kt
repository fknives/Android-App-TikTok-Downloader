package org.fnives.tiktokdownloader.di.module

import android.content.Context
import org.fnives.tiktokdownloader.ui.permission.PermissionChecker

class PermissionModule(context: Context) {

    val permissionChecker: PermissionChecker by lazy {
        PermissionChecker(context.applicationContext)
    }
}
