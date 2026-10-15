package org.fnives.tiktokdownloader.ui.service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fnives.tiktokdownloader.data.usecase.GetUserPreferences
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.ui.main.MainActivity
import org.fnives.tiktokdownloader.ui.permission.Permission

class DownloadIntentReceiverActivity : AppCompatActivity() {

    private val getUserPreferences: GetUserPreferences by lazy {
        ServiceLocator.useCaseModule.getUserPreferences
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
            if (getUserPreferences().alwaysOpenApp) {
                alwaysOpen(url)
            } else {
                openOnlyIfNeeded(url)
            }
        }
        super.onCreate(savedInstanceState)
        finish()
    }

    private fun alwaysOpen(url: String) {
        startActivity(MainActivity.buildIntent(this, url))
    }

    private fun openOnlyIfNeeded(url: String) {
        if (ServiceLocator.permissionChecker.isGranted(Permission.STORAGE)) {
            startService(QueueService.buildIntent(this, url))
        } else {
            startActivity(MainActivity.buildIntent(this, url))
        }
    }
}