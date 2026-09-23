package org.fnives.tiktokdownloader.di.module

import org.fnives.tiktokdownloader.ui.permission.PermissionRationaleDialogFactory
import org.fnives.tiktokdownloader.ui.permission.PermissionRequester

class PermissionModule {

    private val permissionRationaleDialogFactory: PermissionRationaleDialogFactory
        get() = PermissionRationaleDialogFactory()

    private val permissionRequesterFactory: PermissionRequester.Factory
        get() = PermissionRequester.Factory(permissionRationaleDialogFactory)

    val permissionRequester: PermissionRequester
        get() = permissionRequesterFactory.invoke()
}