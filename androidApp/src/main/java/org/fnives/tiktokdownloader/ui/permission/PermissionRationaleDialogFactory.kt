package org.fnives.tiktokdownloader.ui.permission

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.fnives.tiktokdownloader.R

class PermissionRationaleDialogFactory {

    fun show(
        context: Context,
        @StringRes title: Int = R.string.permission_request,
        @StringRes message: Int = R.string.permission_rationale,
        onOkClicked: () -> Unit,
        onCanceled: () -> Unit
    ) {
        var okClicked = false
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                okClicked = true
                onOkClicked()
                dialog.dismiss()
            }
            .setOnDismissListener {
                if (!okClicked) {
                    onCanceled()
                }
            }
            .show()
    }
}
