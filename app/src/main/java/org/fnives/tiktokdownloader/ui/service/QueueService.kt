package org.fnives.tiktokdownloader.ui.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.ui.main.MainActivity


class QueueService : Service() {

    private val viewModel: QueueServiceViewModel by lazy { ServiceLocator.queueServiceViewModel }
    private val serviceLifecycle = ServiceLifecycle()

    override fun onCreate() {
        super.onCreate()
        serviceLifecycle.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        viewModel.notificationState.observe(serviceLifecycle) {
            updateNotification(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.url?.let(viewModel::onUrlReceived)
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.tik_tok_downloader_notification_channel),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        startForeground(
            SERVICE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.tik_tok_downloader_started))
                .setSmallIcon(R.drawable.ic_download)
                .build()
        )
    }

    private fun updateNotification(notificationState: NotificationState) {
        if (notificationState is NotificationState.Finish) {
            stopSelf()
            return
        }
        val (id, notification) = when (notificationState) {
            is NotificationState.Processing ->
                SERVICE_NOTIFICATION_ID to NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.tik_tok_downloader_processing, notificationState.url))
                    .setSmallIcon(R.drawable.ic_download)
                    .setProgress(0, 10, true)
                    .build()
            is NotificationState.Error ->
                NOTIFICATION_ID to NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(notificationState.errorRes))
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentIntent(buildMainPendingIntent(this))
                    .setAutoCancel(true)
                    .setNotificationSilent()
                    .build()
            NotificationState.Finish -> {
                stopSelf()
                return
            }
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
        if (id == NOTIFICATION_ID) {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceLifecycle.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModel.onClear()
    }

    private class ServiceLifecycle : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    companion object {

        private const val CHANNEL_ID = "org.fnives.tiktokdownloader.CHANNEL_ID"
        private const val NOTIFICATION_ID = 420
        private const val SERVICE_NOTIFICATION_ID = 421
        private const val NOTIFICATION_PENDING_INTENT_REQUEST_CODE = 422

        private var Intent.url: String
            get() = getStringExtra("URL").orEmpty()
            set(value) {
                putExtra("URL", value)
            }

        fun buildIntent(context: Context, url: String) =
            Intent(context, QueueService::class.java).also { it.url = url }

        fun buildIntent(context: Context) =
            Intent(context, QueueService::class.java)

        private fun buildMainPendingIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_PENDING_INTENT_REQUEST_CODE,
                MainActivity.buildIntent(context),
                FLAG_UPDATE_CURRENT
            )
    }

}
