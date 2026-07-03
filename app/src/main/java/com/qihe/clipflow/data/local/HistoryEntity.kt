package com.qihe.clipflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,               // 原始链接
    val title: String,             // 标题
    val platform: String,          // "douyin" / "xiaohongshu"
    val coverUrl: String? = null,  // 封面缩略图
    val authorName: String? = null, // 作者
    val contentType: String? = null, // video / image / live / note
    val timestamp: Long = System.currentTimeMillis()
)
