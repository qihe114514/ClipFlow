package com.qihe.clipflow.ui.parser

import android.app.Application
import android.content.ClipboardManager
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.local.AppDatabase
import com.qihe.clipflow.data.repository.HistoryRepository
import com.qihe.clipflow.data.repository.HistorySaver
import com.qihe.clipflow.data.repository.ParseErrorKind
import com.qihe.clipflow.data.repository.ParseException
import com.qihe.clipflow.data.repository.ParseFailure
import com.qihe.clipflow.data.repository.ParseRepository
import com.qihe.clipflow.data.repository.ParseResult
import com.qihe.clipflow.data.repository.PlatformRegistry
import com.qihe.clipflow.data.repository.DouyinParseRoute
import com.qihe.clipflow.data.repository.SupportedPlatform
import com.qihe.clipflow.util.DownloadCoordinator
import com.qihe.clipflow.util.DownloadSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class ParseScreenSupport(
    private val application: Application,
    platform: SupportedPlatform,
    scope: CoroutineScope
) {
    private val descriptor = PlatformRegistry.descriptor(platform)
    private val parseRepository = ParseRepository()
    private val historySaver = HistorySaver(
        HistoryRepository(AppDatabase.getInstance(application).historyDao())
    )
    private val downloadCoordinator = DownloadCoordinator(
        context = application,
        sourceRoute = descriptor.historyKey,
        scope = scope
    )

    val downloadSession: StateFlow<DownloadSessionState> = downloadCoordinator.session

    fun readClipboardText(): String {
        val clipboard = application
            .getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        return runCatching { clip.getItemAt(0)?.text?.toString().orEmpty() }.getOrDefault("")
    }

    fun emptyInputMessage(): String {
        return descriptor.errorMessage(ParseFailure(ParseErrorKind.EMPTY_INPUT))
    }

    fun normalizeInput(rawInput: String): String {
        return parseRepository.normalize(descriptor.platform, rawInput)
    }

    suspend fun parse(
        rawInput: String,
        route: DouyinParseRoute = DouyinParseRoute.PRIMARY
    ): Result<ParseResult> {
        return parseRepository.parse(descriptor.platform, rawInput, route)
    }

    suspend fun saveHistory(sourceUrl: String, result: ParseResult) {
        historySaver.save(
            platform = descriptor.historyKey,
            sourceUrl = sourceUrl,
            result = result,
            defaultTitle = descriptor.defaultTitle(result),
            fallbackContentType = descriptor.fallbackContentType
        )
    }

    fun startDownload(item: ContentItem) {
        downloadCoordinator.startDownload(item)
    }

    fun cancelDownload(itemId: String?) {
        downloadCoordinator.cancel(itemId)
    }

    fun dismissDownloadDialog(background: Boolean) {
        downloadCoordinator.dismiss(background)
    }

    fun userMessage(error: Throwable): String {
        return if (error is ParseException) {
            descriptor.errorMessage(error.failure)
        } else {
            error.message ?: "解析失败"
        }
    }
}
