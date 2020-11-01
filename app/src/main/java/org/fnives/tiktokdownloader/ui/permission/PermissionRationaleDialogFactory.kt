package org.fnives.tiktokdownloader.ui.permission

import android.content.Context
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CompletableDeferred
import org.fnives.tiktokdownloader.R

class PermissionRationaleDialogFactory {

    fun show(context: Context, onOkClicked: () -> Unit, onCanceled: () -> Unit) {
        var okClicked = false
        AlertDialog.Builder(context)
            .setTitle(R.string.permission_request)
            .setMessage(R.string.permission_rationale)
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