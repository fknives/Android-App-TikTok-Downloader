package org.fnives.tiktokdownloader.ui.permission

import androidx.appcompat.app.AppCompatActivity

class PermissionRequesterAbove28 : PermissionRequester {

    override fun invoke(activity: AppCompatActivity) {
        // nothing to do, no permission is required
    }

    override fun isGranted(activity: AppCompatActivity): Boolean = true
}