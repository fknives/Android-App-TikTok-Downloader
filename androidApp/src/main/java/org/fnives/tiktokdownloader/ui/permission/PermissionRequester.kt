package org.fnives.tiktokdownloader.ui.permission

interface PermissionRequester {

    fun isGranted(permission: Permission): Boolean

    fun request(permission: Permission)
}
