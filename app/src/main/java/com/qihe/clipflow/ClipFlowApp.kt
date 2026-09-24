package com.qihe.clipflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import com.umeng.commonsdk.UMConfigure

class ClipFlowApp : Application() {

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "clipflow_downloads"
        const val DOWNLOAD_CHANNEL_NAME = "下载通知"
        private const val UMENG_APPKEY = "6a47c9256f259537c7c319d1"
        private const val UMENG_CHANNEL = "official"
    }

    /** 防止重复 init */
    private var umengInitialized = false

    override fun onCreate() {
        super.onCreate()
        BilibiliSessionStore.initialize(this)
        createNotificationChannels()
        // 合规：友盟 preInit/init 延后到用户同意隐私政策后再执行（见 initUmengIfNeeded）。
        UMConfigure.setLogEnabled(false)
    }

    /**
     * 正式初始化友盟 SDK。必须在用户同意隐私政策后调用。
     * 多次调用安全（仅首次生效）。
     */
    fun initUmengIfNeeded() {
        if (umengInitialized) return
        umengInitialized = true

        Thread {
            UMConfigure.preInit(this, UMENG_APPKEY, UMENG_CHANNEL)
            UMConfigure.init(
                this,
                UMENG_APPKEY,
                UMENG_CHANNEL,
                UMConfigure.DEVICE_TYPE_PHONE,
                null
            )
            UMConfigure.submitPolicyGrantResult(this, true)
        }.start()
    }

    /**
     * 用户在“关于”页撤回隐私同意后调用：通知友盟停止后续采集。
     * 再次同意时会重新走 initUmengIfNeeded()。
     */
    fun revokeUmengConsent() {
        umengInitialized = false
        runCatching { UMConfigure.submitPolicyGrantResult(this, false) }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示下载进度"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
