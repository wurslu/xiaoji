package com.example.thebest.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.thebest.data.repository.SensorRepository
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * 自动清理管理器
 * 负责定期清理旧数据和导出文件缓存
 */
class AutoCleanupManager(
    private val context: Context,
    private val repository: SensorRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cleanupJob: Job? = null

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auto_cleanup", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LAST_CLEANUP = "last_cleanup_time"
        private const val KEY_CLEANUP_ENABLED = "cleanup_enabled"
        private const val KEY_CLEANUP_INTERVAL_DAYS = "cleanup_interval_days"

        // 默认设置
        private const val DEFAULT_CLEANUP_ENABLED = true
        private const val DEFAULT_CLEANUP_INTERVAL_DAYS = 30
        private const val CHECK_INTERVAL_HOURS = 6L // 每6小时检查一次
    }

    /**
     * 启动自动清理服务
     */
    fun startAutoCleanup() {
        cleanupJob?.cancel()

        cleanupJob = scope.launch {
            while (isActive) {
                try {
                    checkAndPerformCleanup()
                } catch (e: Exception) {
                    // 记录错误但不停止服务
                    e.printStackTrace()
                }

                // 等待下次检查
                delay(TimeUnit.HOURS.toMillis(CHECK_INTERVAL_HOURS))
            }
        }
    }

    /**
     * 停止自动清理服务
     */
    fun stopAutoCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
    }

    /**
     * 检查并执行清理任务
     */
    private suspend fun checkAndPerformCleanup() {
        val isEnabled = prefs.getBoolean(KEY_CLEANUP_ENABLED, DEFAULT_CLEANUP_ENABLED)
        if (!isEnabled) return

        val intervalDays = prefs.getInt(KEY_CLEANUP_INTERVAL_DAYS, DEFAULT_CLEANUP_INTERVAL_DAYS)
        val lastCleanup = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        val now = System.currentTimeMillis()

        val shouldCleanup = now - lastCleanup > TimeUnit.DAYS.toMillis(intervalDays.toLong())

        if (shouldCleanup) {
            performCleanup(intervalDays)
            updateLastCleanupTime(now)
        }
    }

    /**
     * 执行清理操作
     */
    private suspend fun performCleanup(retentionDays: Int) {
        try {
            // 1. 清理数据库中的旧数据
            repository.cleanOldRecords(retentionDays)

            // 2. 清理导出文件缓存
            DataExportUtils.cleanOldExports(context)

            // 3. 清理应用缓存
            clearAppCache()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 清理应用缓存
     */
    private fun clearAppCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && shouldDeleteCacheFile(file)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 判断是否应该删除缓存文件
     */
    private fun shouldDeleteCacheFile(file: java.io.File): Boolean {
        val daysSinceModified =
            (System.currentTimeMillis() - file.lastModified()) / (24 * 60 * 60 * 1000L)
        return daysSinceModified > 7 // 删除7天前的缓存文件
    }

    /**
     * 更新最后清理时间
     */
    private fun updateLastCleanupTime(time: Long) {
        prefs.edit()
            .putLong(KEY_LAST_CLEANUP, time)
            .apply()
    }

    /**
     * 手动触发清理
     */
    suspend fun performManualCleanup() {
        val intervalDays = prefs.getInt(KEY_CLEANUP_INTERVAL_DAYS, DEFAULT_CLEANUP_INTERVAL_DAYS)
        performCleanup(intervalDays)
        updateLastCleanupTime(System.currentTimeMillis())
    }

    /**
     * 更新清理设置
     */
    fun updateCleanupSettings(enabled: Boolean, intervalDays: Int) {
        prefs.edit()
            .putBoolean(KEY_CLEANUP_ENABLED, enabled)
            .putInt(KEY_CLEANUP_INTERVAL_DAYS, intervalDays)
            .apply()

        // 如果启用了自动清理但服务未运行，则启动服务
        if (enabled && cleanupJob?.isActive != true) {
            startAutoCleanup()
        }
        // 如果禁用了自动清理，则停止服务
        else if (!enabled) {
            stopAutoCleanup()
        }
    }

    /**
     * 获取清理统计信息
     */
    suspend fun getCleanupStats(): CleanupStats {
        val lastCleanup = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        val isEnabled = prefs.getBoolean(KEY_CLEANUP_ENABLED, DEFAULT_CLEANUP_ENABLED)
        val intervalDays = prefs.getInt(KEY_CLEANUP_INTERVAL_DAYS, DEFAULT_CLEANUP_INTERVAL_DAYS)

        val nextCleanup = if (isEnabled && lastCleanup > 0) {
            lastCleanup + TimeUnit.DAYS.toMillis(intervalDays.toLong())
        } else 0L

        return CleanupStats(
            isEnabled = isEnabled,
            lastCleanupTime = lastCleanup,
            nextCleanupTime = nextCleanup,
            intervalDays = intervalDays
        )
    }

    /**
     * 计算需要清理的数据量
     */
    suspend fun calculateCleanupSize(): CleanupSize {
        val intervalDays = prefs.getInt(KEY_CLEANUP_INTERVAL_DAYS, DEFAULT_CLEANUP_INTERVAL_DAYS)
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(intervalDays.toLong())

        // 计算将被删除的记录数
        val oldRecordCount = repository.getRecordCountBefore(cutoffTime)

        // 估算数据大小（每条记录约60字节）
        val estimatedSize = oldRecordCount * 60L

        // 计算导出文件大小
        val exportFilesSize = calculateExportFilesSize()

        return CleanupSize(
            recordCount = oldRecordCount,
            databaseSize = estimatedSize,
            exportFilesSize = exportFilesSize,
            totalSize = estimatedSize + exportFilesSize
        )
    }

    /**
     * 计算导出文件大小
     */
    private fun calculateExportFilesSize(): Long {
        return try {
            val exportDir = java.io.File(context.getExternalFilesDir(null), "sensor_exports")
            if (exportDir.exists()) {
                exportDir.walkTopDown()
                    .filter { it.isFile }
                    .filter { shouldDeleteExportFile(it) }
                    .sumOf { it.length() }
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 判断是否应该删除导出文件
     */
    private fun shouldDeleteExportFile(file: java.io.File): Boolean {
        val daysSinceModified =
            (System.currentTimeMillis() - file.lastModified()) / (24 * 60 * 60 * 1000L)
        return daysSinceModified > 30 // 删除30天前的导出文件
    }

    /**
     * 释放资源
     */
    fun destroy() {
        scope.cancel()
    }
}

/**
 * 清理统计信息
 */
data class CleanupStats(
    val isEnabled: Boolean,
    val lastCleanupTime: Long,
    val nextCleanupTime: Long,
    val intervalDays: Int
)

/**
 * 清理大小信息
 */
data class CleanupSize(
    val recordCount: Int,
    val databaseSize: Long,
    val exportFilesSize: Long,
    val totalSize: Long
)