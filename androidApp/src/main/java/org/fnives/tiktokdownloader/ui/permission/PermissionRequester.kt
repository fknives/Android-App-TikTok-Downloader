package org.fnives.tiktokdownloader.ui.permission

import android.os.Build
import androidx.appcompat.app.AppCompatActivity

interface PermissionRequester {

    operator fun invoke(activity: AppCompatActivity)

    fun isGranted(activity: AppCompatActivity): Boolean

    class Factory(private val permissionRationaleDialogFactory: PermissionRationaleDialogFactory) {

        fun invoke(): PermissionRequester =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                PermissionRequesterAbove28()
            } else {
                PermissionRequesterBelow28(permissionRationaleDialogFactory)
            }
    }
}