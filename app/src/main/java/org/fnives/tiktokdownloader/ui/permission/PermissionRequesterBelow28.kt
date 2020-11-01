package org.fnives.tiktokdownloader.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.ui.permission.PermissionRequesterBelow28.Companion.hasPermission

class PermissionRequesterBelow28(
    private val permissionRationaleDialogFactory: PermissionRationaleDialogFactory
) : PermissionRequester {

    override operator fun invoke(activity: AppCompatActivity) {
        val interactor = PermissionLauncherInteractor(activity, permissionRationaleDialogFactory)
        interactor.attach()
    }

    override fun isGranted(activity: AppCompatActivity): Boolean =
        activity.hasPermission(STORAGE_PERMISSION)


    private class PermissionLauncherInteractor(
        private val activity: AppCompatActivity,
        private val permissionRationaleDialogFactory: PermissionRationaleDialogFactory
    ) : LifecycleEventObserver {
        val requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted && activity.shouldShowRequestPermissionRationale(STORAGE_PERMISSION)) {
                showRationale()
            } else {
                onFinalResponse(isGranted)
            }
        }

        fun attach() {
            activity.lifecycle.addObserver(this)
        }

        private fun checkOrRequestPermission() {
            when {
                activity.hasPermission(STORAGE_PERMISSION) -> onFinalResponse(true)
                activity.shouldShowRequestPermissionRationale(STORAGE_PERMISSION) -> showRationale()
                else -> requestPermissionLauncher.launch(STORAGE_PERMISSION)
            }
        }

        private fun showRationale() {
            permissionRationaleDialogFactory.show(
                activity,
                onOkClicked = {
                    requestPermissionLauncher.launch(STORAGE_PERMISSION)
                },
                onCanceled = {
                    onFinalResponse(false)
                }
            )
        }

        private fun onFinalResponse(isGranted: Boolean) {
            if (!isGranted) {
                Toast.makeText(activity, R.string.cant_operate_without_permission, Toast.LENGTH_LONG).show()
                activity.finishAffinity()
            }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_START) {
                checkOrRequestPermission()
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                source.lifecycle.removeObserver(this)
                requestPermissionLauncher.unregister()
            }
        }
    }

    companion object {

        private const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

        private fun Context.hasPermission(permission: String): Boolean =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}