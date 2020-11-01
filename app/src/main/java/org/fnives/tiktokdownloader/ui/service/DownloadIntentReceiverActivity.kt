package org.fnives.tiktokdownloader.ui.service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.ui.main.MainActivity
import org.fnives.tiktokdownloader.ui.permission.PermissionRequester

class DownloadIntentReceiverActivity : AppCompatActivity() {

    private val permissionRequester: PermissionRequester by lazy {
        ServiceLocator.permissionModule.permissionRequester
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
            if (permissionRequester.isGranted(this)) {
                startService(QueueService.buildIntent(this, url))
            } else {
                startActivity(MainActivity.buildIntent(this, url))
            }
        }
        super.onCreate(savedInstanceState)
        finish()
    }
}