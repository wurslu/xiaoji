package com.example.thebest.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.local.SensorRecord
import com.example.thebest.data.repository.SensorRepository
import com.example.thebest.ui.compose.*
import com.example.thebest.utils.DataExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataManagementViewModel(
    private val repository: SensorRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // SharedPreferences for data management settings
    private val dataPrefs: SharedPreferences? = context?.getSharedPreferences(
        "data_management", Context.MODE_PRIVATE
    )

    data class UiState(
        val dataStatistics: DataStatistics = DataStatistics(),
        val storageInfo: StorageInfo = StorageInfo(),
        val isLoading: Boolean = false,
        val isProcessing: Boolean = false,
        val isExporting: Boolean = false,
        val operationMessage: String = "",
        val operationSuccess: Boolean = false,
        val isAutoCleanupEnabled: Boolean = true,
        val autoCleanupDays: Int = 30
    )

    init {
        loadSettings()
        loadDataStatistics()
    }

    private fun loadSettings() {
        dataPrefs?.let { prefs ->
            _uiState.value = _uiState.value.copy(
                isAutoCleanupEnabled = prefs.getBoolean("auto_cleanup_enabled", true),
                autoCleanupDays = prefs.getInt("auto_cleanup_days", 30)
            )
        }
    }

    fun loadDataStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val statistics = calculateDataStatistics()
                val storageInfo = calculateStorageInfo()

                _uiState.value = _uiState.value.copy(
                    dataStatistics = statistics,
                    storageInfo = storageInfo,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "加载数据统计失败: ${e.message}",
                    operationSuccess = false
                )
            }
        }
    }

    private suspend fun calculateDataStatistics(): DataStatistics = withContext(Dispatchers.IO) {
        try {
            val totalRecords = repository.getRecordCount()

            if (totalRecords == 0) {
                return@withContext DataStatistics()
            }

            // 计算30天前的数据
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            val oldDataCount = repository.getRecordCountBefore(thirtyDaysAgo)

            // 获取时间范围
            val oldestTimestamp = repository.getOldestRecordTimestamp()
            val newestTimestamp = repository.getNewestRecordTimestamp()

            val daysCovered = if (oldestTimestamp != null && newestTimestamp != null) {
                ((newestTimestamp - oldestTimestamp) / (24 * 60 * 60 * 1000L)).toInt() + 1
            } else 0

            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val oldestRecord = oldestTimestamp?.let {
                dateFormat.format(Date(it))
            }
            val newestRecord = newestTimestamp?.let {
                dateFormat.format(Date(it))
            }

            DataStatistics(
                totalRecords = totalRecords,
                daysCovered = daysCovered,
                oldDataCount = oldDataCount,
                oldestRecord = oldestRecord,
                newestRecord = newestRecord
            )
        } catch (e: Exception) {
            DataStatistics()
        }
    }

    private suspend fun calculateStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        if (context == null) return@withContext StorageInfo()

        val databaseSize = getDatabaseSize(context)
        val exportFilesSize = getExportFilesSize(context)
        val cacheSize = getCacheSize(context)

        StorageInfo(
            databaseSize = databaseSize,
            exportFilesSize = exportFilesSize,
            cacheSize = cacheSize,
            totalSize = databaseSize + exportFilesSize + cacheSize
        )
    }

    private fun getDatabaseSize(context: Context): Long {
        return try {
            val dbFile = context.getDatabasePath("sensor_database")
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getExportFilesSize(context: Context): Long {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "sensor_exports")
            if (exportDir.exists()) {
                exportDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCacheSize(context: Context): Long {
        return try {
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    fun clearData(clearType: ClearDataType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                when (clearType) {
                    ClearDataType.OLD_DATA -> clearOldData()
                    ClearDataType.ALL -> clearAllData()
                }

                // 重新加载统计信息
                loadDataStatistics()

                val message = when (clearType) {
                    ClearDataType.OLD_DATA -> "旧数据清理完成"
                    ClearDataType.ALL -> "所有数据已清空"
                }

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    operationMessage = message,
                    operationSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    operationMessage = "数据清理失败: ${e.message}",
                    operationSuccess = false
                )
            }
        }
    }

    private suspend fun clearOldData() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        repository.cleanOldRecords(30)
    }

    private suspend fun clearAllData() {
        repository.clearAllRecords()
    }

    // 数据导出功能
    fun exportData(exportType: ExportType) {
        viewModelScope.launch {
            if (context == null) return@launch

            _uiState.value = _uiState.value.copy(isExporting = true)

            try {
                val records = getRecordsForExport(exportType)
                val fileName = getExportFileName(exportType)

                val result = DataExportUtils.exportToCSV(context, records, fileName)

                result.fold(
                    onSuccess = { filePath ->
                        DataExportUtils.shareExportedFile(context, filePath)
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            operationMessage = "数据导出成功！",
                            operationSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            operationMessage = "导出失败: ${error.message}",
                            operationSuccess = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    operationMessage = "导出失败: ${e.message}",
                    operationSuccess = false
                )
            }
        }
    }

    fun exportRecentData() {
        exportData(ExportType.LAST_7_DAYS)
    }

    private suspend fun getRecordsForExport(exportType: ExportType): List<SensorRecord> {
        val now = System.currentTimeMillis()

        return when (exportType) {
            ExportType.ALL -> {
                // 修复：使用 first() 而不是 collect
                try {
                    repository.getHistoryRecords().first()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            ExportType.LAST_7_DAYS -> {
                val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
                try {
                    repository.getRecordsByDateRange(sevenDaysAgo, now).first()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            ExportType.LAST_30_DAYS -> {
                val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
                try {
                    repository.getRecordsByDateRange(thirtyDaysAgo, now).first()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            ExportType.LAST_90_DAYS -> {
                val ninetyDaysAgo = now - (90 * 24 * 60 * 60 * 1000L)
                try {
                    repository.getRecordsByDateRange(ninetyDaysAgo, now).first()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    private fun getExportFileName(exportType: ExportType): String {
        return when (exportType) {
            ExportType.ALL -> "sensor_data_all"
            ExportType.LAST_7_DAYS -> "sensor_data_7days"
            ExportType.LAST_30_DAYS -> "sensor_data_30days"
            ExportType.LAST_90_DAYS -> "sensor_data_90days"
        }
    }

    // 自动清理设置
    fun toggleAutoCleanup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoCleanupEnabled = enabled)
        saveSettings()
    }

    fun updateAutoCleanupDays(days: Int) {
        _uiState.value = _uiState.value.copy(autoCleanupDays = days)
        saveSettings()
    }

    private fun saveSettings() {
        dataPrefs?.edit()?.apply {
            putBoolean("auto_cleanup_enabled", _uiState.value.isAutoCleanupEnabled)
            putInt("auto_cleanup_days", _uiState.value.autoCleanupDays)
            apply()
        }
    }

    fun clearOperationMessage() {
        _uiState.value = _uiState.value.copy(
            operationMessage = "",
            operationSuccess = false
        )
    }

    // 执行自动清理（供定时任务调用）
    fun performAutoCleanup() {
        if (!_uiState.value.isAutoCleanupEnabled) return

        viewModelScope.launch {
            try {
                val cutoffTime =
                    System.currentTimeMillis() - (_uiState.value.autoCleanupDays * 24 * 60 * 60 * 1000L)
                // 使用 repository 的清理方法
                repository.cleanOldRecords()
            } catch (e: Exception) {
                // 静默处理自动清理错误
            }
        }
    }

    // 清理导出文件缓存
    fun cleanExportCache() {
        viewModelScope.launch {
            if (context == null) return@launch

            try {
                DataExportUtils.cleanOldExports(context)

                // 重新加载存储信息
                val storageInfo = calculateStorageInfo()
                _uiState.value = _uiState.value.copy(
                    storageInfo = storageInfo,
                    operationMessage = "导出文件缓存已清理",
                    operationSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    operationMessage = "清理缓存失败: ${e.message}",
                    operationSuccess = false
                )
            }
        }
    }

    companion object {
        // 获取自动清理设置的静态方法
        fun getAutoCleanupSettings(context: Context): Pair<Boolean, Int> {
            val prefs = context.getSharedPreferences("data_management", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("auto_cleanup_enabled", true)
            val days = prefs.getInt("auto_cleanup_days", 30)
            return Pair(enabled, days)
        }
    }
}