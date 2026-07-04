package com.qihe.clipflow.ui.history

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qihe.clipflow.data.local.AppDatabase
import com.qihe.clipflow.data.local.HistoryEntity
import com.qihe.clipflow.data.repository.HistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class HistoryUiState(
    val items: List<HistoryEntity> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showDeleteConfirm: Boolean = false,
    val toastMessage: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val historyRepository = HistoryRepository(
        AppDatabase.getInstance(application).historyDao()
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .flatMapLatest { query ->
                    if (query.isBlank()) historyRepository.getAll()
                    else historyRepository.search(query)
                }
                .collect { items ->
                    _uiState.value = _uiState.value.copy(items = items)
                }
        }
    }

    fun toggleSelectionMode() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isSelectionMode = !current.isSelectionMode,
            selectedIds = emptySet()
        )
    }

    fun toggleItemSelection(id: Long) {
        val current = _uiState.value
        val newSelected = if (id in current.selectedIds) current.selectedIds - id
        else current.selectedIds + id
        _uiState.value = current.copy(selectedIds = newSelected)
    }

    fun selectAll() {
        val current = _uiState.value
        _uiState.value = current.copy(selectedIds = current.items.map { it.id }.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun requestDelete() {
        if (_uiState.value.selectedIds.isNotEmpty())
            _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun confirmDelete() {
        viewModelScope.launch {
            historyRepository.deleteByIds(_uiState.value.selectedIds.toList())
            _uiState.value = _uiState.value.copy(
                showDeleteConfirm = false, isSelectionMode = false, selectedIds = emptySet()
            )
        }
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    // ========== 导出 ==========

    fun exportSelected(exportUri: Uri) {
        viewModelScope.launch {
            try {
                val selected = _uiState.value.items.filter { it.id in _uiState.value.selectedIds }
                val jsonArray = JSONArray()
                selected.forEach { item ->
                    jsonArray.put(JSONObject().apply {
                        put("url", item.url)
                        put("title", item.title)
                        put("platform", item.platform)
                        put("coverUrl", item.coverUrl ?: "")
                        put("authorName", item.authorName ?: "")
                        put("contentType", item.contentType ?: "")
                        put("timestamp", item.timestamp)
                    })
                }

                app.contentResolver.openOutputStream(exportUri)?.use { output ->
                    output.write(jsonArray.toString(2).toByteArray())
                }
                _uiState.value = _uiState.value.copy(
                    toastMessage = "已导出 ${selected.size} 条记录",
                    isSelectionMode = false, selectedIds = emptySet()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "导出失败: ${e.message}")
            }
        }
    }

    // ========== 导入 ==========

    fun importFromUri(importUri: Uri) {
        viewModelScope.launch {
            try {
                val jsonText = app.contentResolver.openInputStream(importUri)?.bufferedReader()?.readText()
                    ?: throw Exception("无法读取文件")
                val jsonArray = JSONArray(jsonText)
                val entities = mutableListOf<HistoryEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        HistoryEntity(
                            url = obj.getString("url"),
                            title = obj.optString("title", ""),
                            platform = obj.getString("platform"),
                            coverUrl = obj.optString("coverUrl", "").ifEmpty { null },
                            authorName = obj.optString("authorName", "").ifEmpty { null },
                            contentType = obj.optString("contentType", "").ifEmpty { null },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                entities.forEach { historyRepository.insert(it) }
                _uiState.value = _uiState.value.copy(
                    toastMessage = "已导入 ${entities.size} 条记录"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toastMessage = "导入失败: ${e.message}")
            }
        }
    }
}
