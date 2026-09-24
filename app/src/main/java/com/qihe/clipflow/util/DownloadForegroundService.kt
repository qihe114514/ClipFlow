package com.qihe.clipflow.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.qihe.clipflow.ClipFlowApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 下载期间的前台服务：避免 Android 12+ 应用退后台被冻结导致下载停滞。
 * 真正的下载仍由各 ViewModel/DownloadManager 执行，本服务只负责保活与统一进度通知。
 * 若系统拒绝启动前台服务（如后台启动限制/权限缺失），静默降级，不影响下载逻辑本身。
 */
class DownloadForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false
    private var lastNotificationAt = 0L
    private var lastActiveCount = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            combine(
                DownloadSessionTracker.activeCount,
                DownloadSessionTracker.progress,
                DownloadSessionTracker.statusText,
            ) { count, progress, status -> Triple(count, progress, status) }
                .collect { (count, progress, status) ->
                    if (count <= 0) {
                        stopForegroundCompat()
                        stopSelf()
                    } else {
                        // 进度回调约 200ms 一次，通知节流到 500ms，避免频繁 binder 与状态栏刷新。
                        val now = SystemClock.elapsedRealtime()
                        val countChanged = count != lastActiveCount
                        if (!foregroundStarted || countChanged || now - lastNotificationAt >= 500L) {
                            lastActiveCount = count
                            lastNotificationAt = now
                            startForegroundCompat(buildNotification(progress, status))
                        }
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(
            buildNotification(
                DownloadSessionTracker.progress.value,
                DownloadSessionTracker.statusText.value,
            )
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // 权限由系统在 startForeground 时校验；这里显式捕获 SecurityException 并降级。
    @SuppressLint("MissingPermission")
    private fun startForegroundCompat(notification: Notification) {
        if (foregroundStarted) {
            runCatching {
                NotificationManagerCompat.from(this).notify(FOREGROUND_NOTIFICATION_ID, notification)
            }
            return
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        val started = try {
            ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, type)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!started) {
            stopSelf()
            return
        }
        foregroundStarted = true
    }

    private fun stopForegroundCompat() {
        if (!foregroundStarted) return
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        foregroundStarted = false
    }

    private fun buildNotification(progress: Float, status: String): Notification {
        val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
        return NotificationCompat.Builder(this, ClipFlowApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("ClipFlow 正在下载")
            .setContentText(status)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(DownloadNotifier.contentIntent(this))
            .build()
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 0x434C0F01

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            runCatching { ContextCompat.startForegroundService(context.applicationContext, intent) }
        }
    }
}
