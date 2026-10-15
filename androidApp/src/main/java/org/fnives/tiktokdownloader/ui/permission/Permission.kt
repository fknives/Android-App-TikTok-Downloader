package org.fnives.tiktokdownloader.ui.permission

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import org.fnives.tiktokdownloader.R

/**
 * The permissions the app can ask for, described in app terms only. Everything Android specific
 * (the platform permission string and the SDK versions it applies to) is resolved by
 * [androidPermission], so the enum never stores a platform permission constant.
 *
 * @param isMandatory whether denying it makes the app unusable (in which case the app is closed).
 * @param alwaysShowRationale whether the explanation dialog should be shown before the system
 * dialog even on the first request (used to explain non-obvious permissions like notifications).
 */
enum class Permission(
    val isMandatory: Boolean,
    val alwaysShowRationale: Boolean,
    @StringRes val rationaleTitle: Int,
    @StringRes val rationaleMessage: Int,
) {

    STORAGE(
        isMandatory = true,
        alwaysShowRationale = false,
        rationaleTitle = R.string.permission_request,
        rationaleMessage = R.string.permission_rationale,
    ),

    NOTIFICATION(
        isMandatory = false,
        alwaysShowRationale = true,
        rationaleTitle = R.string.notification_permission_request,
        rationaleMessage = R.string.notification_permission_rationale,
    ),
}

/**
 * Returns the platform permission if it makes sense to request it on this SDK version, otherwise
 * null. Outside the returned versions the permission is considered always granted.
 */
internal fun Permission.androidPermission(): String? = when (this) {
    Permission.STORAGE ->
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            null
        }

    Permission.NOTIFICATION ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
}
