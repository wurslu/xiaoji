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

    // 清理旧数据（保留最近7天）
    suspend fun cleanOldRecords() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        sensorDao.deleteOldRecords(sevenDaysAgo)
    }

    // 获取记录总数
    suspend fun getRecordCount(): Int {
        return sensorDao.getRecordCount()
    }

    // 清除所有历史记录
    suspend fun clearAllRecords() {
        sensorDao.deleteAllRecords()
    }
}