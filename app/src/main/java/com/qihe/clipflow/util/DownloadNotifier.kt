package com.qihe.clipflow.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.MainActivity

/**
 * 下载进度通知。
 * 说明：当前未引入前台服务，因此进程被杀时下载仍会中断；通知用于在应用存活期间提供可见反馈。
 */
object DownloadNotifier {

    private fun notificationId(key: String): Int = "clipflow_download_$key".hashCode()

    internal fun contentIntent(context: Context): PendingIntent? = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )


    @SuppressLint("MissingPermission")
    fun complete(context: Context, key: String, title: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, ClipFlowApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("已保存到相册")
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .build()
        runCatching { manager.notify(notificationId(key), notification) }
    }

    fun cancel(context: Context, key: String) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId(key)) }
    }
}
