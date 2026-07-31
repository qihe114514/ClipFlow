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
            historyRepository.updateTimestamp(sourceUrl, System.currentTimeMillis())
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
