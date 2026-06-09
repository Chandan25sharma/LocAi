package com.locai.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.locai.app.LocAiApplication
import com.locai.app.data.llm.DownloadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground service that keeps model downloads alive even when the user switches away from the
 * app. A persistent notification with a progress bar is shown for the duration of the download.
 *
 * Progress is written to the corresponding [MutableStateFlow] in [com.locai.app.LocAiContainer]
 * so ViewModels can update their UI state reactively without being coupled to the service.
 */
class ModelDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val container get() = (application as LocAiApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val url = intent.getStringExtra(EXTRA_URL) ?: return stopSelfAndReturn(startId)
        val destPath = intent.getStringExtra(EXTRA_DEST) ?: return stopSelfAndReturn(startId)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Downloading"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_VISION

        startForeground(notifIdFor(type), buildNotification(title, indeterminate = true, downloaded = 0L, total = 0L))

        val progressFlow = progressFlowFor(type)

        serviceScope.launch {
            container.modelDownloader.download(url, File(destPath)).collect { progress ->
                progressFlow.value = progress
                when (progress) {
                    is DownloadProgress.InProgress -> {
                        notify(notifIdFor(type), buildNotification(title, indeterminate = progress.totalBytes <= 0, downloaded = progress.bytesDownloaded, total = progress.totalBytes))
                    }
                    is DownloadProgress.Completed -> {
                        when (type) {
                            TYPE_TEXT -> container.preferences.setModelDownloaded(true)
                            TYPE_VISION -> container.preferences.setVisionModelDownloaded(true)
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                    is DownloadProgress.Failed -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun progressFlowFor(type: String): MutableStateFlow<DownloadProgress?> = when (type) {
        TYPE_TEXT -> container.textDownloadProgress
        else -> container.visionDownloadProgress
    }

    private fun stopSelfAndReturn(startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun buildNotification(
        title: String,
        indeterminate: Boolean,
        downloaded: Long,
        total: Long
    ): Notification {
        val progress = if (!indeterminate && total > 0) ((downloaded.toFloat() / total) * 100).toInt() else 0
        val contentText = when {
            indeterminate -> "Starting…"
            total > 0 -> "${downloaded / MB} / ${total / MB} MB"
            downloaded > 0 -> "${downloaded / MB} MB downloaded"
            else -> "Downloading…"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun notify(id: Int, notification: Notification) {
        getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Model Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress while downloading on-device AI models"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "model_downloads"
        const val EXTRA_URL = "url"
        const val EXTRA_DEST = "dest"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TYPE = "type"
        const val TYPE_TEXT = "text"
        const val TYPE_VISION = "vision"
        private const val NOTIF_ID_TEXT = 2001
        private const val NOTIF_ID_VISION = 2002
        private const val MB = 1024 * 1024L

        fun notifIdFor(type: String) = if (type == TYPE_TEXT) NOTIF_ID_TEXT else NOTIF_ID_VISION

        fun startFor(context: Context, type: String, url: String, destPath: String, title: String) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_DEST, destPath)
                putExtra(EXTRA_TITLE, title)
            }
            context.startForegroundService(intent)
        }
    }
}
