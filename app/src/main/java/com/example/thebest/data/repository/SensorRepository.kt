package com.example.thebest.data.repository

import com.example.thebest.data.local.SensorDao
import com.example.thebest.data.local.SensorRecord
import com.example.thebest.data.model.SensorData
import com.example.thebest.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class SensorRepository(
    private val apiService: ApiService,
    private val sensorDao: SensorDao
) {

    // 获取数据并保存到本地
    fun getSensorData(): Flow<Result<SensorData>> = flow {
        try {
            val data = apiService.getSensorData()

            // 保存数据到本地数据库
            val record = SensorRecord(
                temperature = data.temperature,
                humidity = data.humidity,
                light = data.light,
                soil = data.soil
            )
            sensorDao.insertSensorRecord(record)

            emit(Result.success(data))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // 仅获取数据，不保存到本地（用于实时刷新）
    fun getSensorDataOnly(): Flow<Result<SensorData>> = flow {
        try {
            val data = apiService.getSensorData()
            emit(Result.success(data))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // 手动保存当前数据到本地
    suspend fun saveSensorData(data: SensorData) {
        try {
            val record = SensorRecord(
                temperature = data.temperature,
                humidity = data.humidity,
                light = data.light,
                soil = data.soil
            )
            sensorDao.insertSensorRecord(record)
        } catch (e: Exception) {
            // 忽略保存错误，不影响实时显示
        }
    }

    // 获取历史记录
    fun getHistoryRecords(): Flow<List<SensorRecord>> {
        return sensorDao.getAllRecords()
    }

    // 获取最近N条记录
    fun getRecentRecords(limit: Int): Flow<List<SensorRecord>> {
        return sensorDao.getRecentRecords(limit)
    }

    // 按日期范围获取记录
    fun getRecordsByDateRange(startTime: Long, endTime: Long): Flow<List<SensorRecord>> {
        return sensorDao.getRecordsByDateRange(startTime, endTime)
    }

    // 获取今天的记录
    fun getTodayRecords(): Flow<List<SensorRecord>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return sensorDao.getRecordsByDateRange(startOfDay, endOfDay)
    }

    // 清理旧数据（保留最近指定天数）
    suspend fun cleanOldRecords(retentionDays: Int = 7) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        sensorDao.deleteOldRecords(cutoffTime)
    }

    // 获取记录总数
    suspend fun getRecordCount(): Int {
        return sensorDao.getRecordCount()
    }

    // 清除所有历史记录
    suspend fun clearAllRecords() {
        sensorDao.deleteAllRecords()
    }

    // 新增：按时间删除记录
    suspend fun deleteRecordsBefore(cutoffTime: Long) {
        sensorDao.deleteOldRecords(cutoffTime)
    }

    // 新增：获取指定时间之前的记录数量
    suspend fun getRecordCountBefore(cutoffTime: Long): Int {
        return sensorDao.getRecordCountBefore(cutoffTime)
    }

    // 新增：获取重复记录数量（基于时间戳）
    suspend fun getDuplicateRecordCount(): Int {
        return sensorDao.getDuplicateRecordCount()
    }

    // 新增：删除重复记录（保留最新的一条）
    suspend fun deleteDuplicateRecords() {
        sensorDao.deleteDuplicateRecords()
    }

    // 新增：获取最旧记录的时间戳
    suspend fun getOldestRecordTimestamp(): Long? {
        return sensorDao.getOldestRecordTimestamp()
    }

    // 新增：获取最新记录的时间戳
    suspend fun getNewestRecordTimestamp(): Long? {
        return sensorDao.getNewestRecordTimestamp()
    }

    // 新增：按批次删除记录（用于大量数据的高效删除）
    suspend fun deleteRecordsInBatches(cutoffTime: Long, batchSize: Int = 1000) {
        var deletedCount: Int
        do {
            deletedCount = sensorDao.deleteOldRecordsBatch(cutoffTime, batchSize)
        } while (deletedCount > 0)
    }

    // 新增：获取数据库统计信息
    suspend fun getDatabaseStats(): DatabaseStats {
        val totalRecords = sensorDao.getRecordCount()
        val oldestTimestamp = sensorDao.getOldestRecordTimestamp()
        val newestTimestamp = sensorDao.getNewestRecordTimestamp()
        val duplicateCount = sensorDao.getDuplicateRecordCount()

        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val oldRecordCount = sensorDao.getRecordCountBefore(thirtyDaysAgo)

        return DatabaseStats(
            totalRecords = totalRecords,
            oldRecordCount = oldRecordCount,
            duplicateCount = duplicateCount,
            oldestTimestamp = oldestTimestamp,
            newestTimestamp = newestTimestamp
        )
    }
}

// 数据库统计信息数据类
data class DatabaseStats(
    val totalRecords: Int,
    val oldRecordCount: Int,
    val duplicateCount: Int,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?
)