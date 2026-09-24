package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.local.HistoryEntity

class HistorySaver(
    private val historyRepository: HistoryRepository
) {

    suspend fun save(
        platform: String,
        sourceUrl: String,
        result: ParseResult,
        defaultTitle: String,
        fallbackContentType: String? = null
    ) {
        val existing = historyRepository.getByUrl(sourceUrl)
        if (existing != null) {
            // 重新解析同一链接时刷新元数据，避免旧的兜底标题/封面一直留着。
            historyRepository.update(
                existing.copy(
                    title = result.title.ifBlank { existing.title },
                    coverUrl = result.cover.ifBlank { existing.coverUrl.orEmpty() }.ifBlank { null },
                    authorName = result.authorName.ifBlank { existing.authorName.orEmpty() }.ifBlank { null },
                    contentType = result.contentType
                        .ifBlank { fallbackContentType ?: existing.contentType.orEmpty() }
                        .ifBlank { null },
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        historyRepository.insert(
            HistoryEntity(
                url = sourceUrl,
                title = result.title.ifEmpty { defaultTitle },
                platform = platform,
                coverUrl = result.cover.ifEmpty { null },
                authorName = result.authorName.ifEmpty { null },
                contentType = result.contentType.ifEmpty { fallbackContentType }
            )
        )
    }
}
