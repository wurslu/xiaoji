package com.example.thebest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {

    @Insert
    suspend fun insertSensorRecord(record: SensorRecord)

    @Query("SELECT * FROM sensor_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SensorRecord>>

    @Query("SELECT * FROM sensor_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<SensorRecord>>

    @Query("SELECT * FROM sensor_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getRecordsByDateRange(startTime: Long, endTime: Long): Flow<List<SensorRecord>>

    @Query("DELETE FROM sensor_records WHERE timestamp < :cutoffTime")
    suspend fun deleteOldRecords(cutoffTime: Long)

    @Query("DELETE FROM sensor_records")
    suspend fun deleteAllRecords()

    @Query("SELECT COUNT(*) FROM sensor_records")
    suspend fun getRecordCount(): Int

    // 新增：获取指定时间之前的记录数量
    @Query("SELECT COUNT(*) FROM sensor_records WHERE timestamp < :cutoffTime")
    suspend fun getRecordCountBefore(cutoffTime: Long): Int

    // 新增：获取重复记录数量（基于时间戳）
    @Query(
        """
        SELECT COUNT(*) - COUNT(DISTINCT timestamp) 
        FROM sensor_records
    """
    )
    suspend fun getDuplicateRecordCount(): Int

    // 新增：删除重复记录（保留每个时间戳的最新记录）
    @Query(
        """
        DELETE FROM sensor_records 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM sensor_records 
            GROUP BY timestamp
        )
    """
    )
    suspend fun deleteDuplicateRecords()

    // 新增：获取最旧记录的时间戳
    @Query("SELECT MIN(timestamp) FROM sensor_records")
    suspend fun getOldestRecordTimestamp(): Long?

    // 新增：获取最新记录的时间戳
    @Query("SELECT MAX(timestamp) FROM sensor_records")
    suspend fun getNewestRecordTimestamp(): Long?

    // 新增：批量删除旧记录（用于大数据量的高效删除）
    @Query(
        """
        DELETE FROM sensor_records 
        WHERE id IN (
            SELECT id FROM sensor_records 
            WHERE timestamp < :cutoffTime 
            LIMIT :batchSize
        )
    """
    )
    suspend fun deleteOldRecordsBatch(cutoffTime: Long, batchSize: Int): Int

    // 新增：获取数据统计信息的高效查询
    @Query(
        """
        SELECT 
            COUNT(*) as total_count,
            MIN(timestamp) as oldest_timestamp,
            MAX(timestamp) as newest_timestamp,
            COUNT(*) - COUNT(DISTINCT timestamp) as duplicate_count
        FROM sensor_records
    """
    )
    suspend fun getRecordStatistics(): RecordStatistics

    // 新增：获取某个时间段内的记录数量
    @Query("SELECT COUNT(*) FROM sensor_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getRecordCountInRange(startTime: Long, endTime: Long): Int

    // 新增：获取平均数据（用于统计）
    @Query(
        """
        SELECT 
            AVG(temperature) as avg_temperature,
            AVG(humidity) as avg_humidity,
            AVG(light) as avg_light,
            AVG(soil) as avg_soil
        FROM sensor_records 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """
    )
    suspend fun getAverageData(startTime: Long, endTime: Long): AverageData?

    // 新增：删除指定数量的最旧记录
    @Query(
        """
        DELETE FROM sensor_records 
        WHERE id IN (
            SELECT id FROM sensor_records 
            ORDER BY timestamp ASC 
            LIMIT :count
        )
    """
    )
    suspend fun deleteOldestRecords(count: Int)

    // 新增：检查是否存在指定时间戳的记录
    @Query("SELECT EXISTS(SELECT 1 FROM sensor_records WHERE timestamp = :timestamp)")
    suspend fun recordExistsAtTimestamp(timestamp: Long): Boolean

    // 新增：获取指定日期的记录数量
    @Query(
        """
        SELECT COUNT(*) FROM sensor_records 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE(:timestamp/1000, 'unixepoch', 'localtime')
    """
    )
    suspend fun getRecordCountForDate(timestamp: Long): Int

    // 新增：获取每日记录数量统计
    @Query(
        """
        SELECT 
            DATE(timestamp/1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as count
        FROM sensor_records 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY DATE(timestamp/1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
    """
    )
    suspend fun getDailyRecordCounts(startTime: Long, endTime: Long): List<DailyCount>
}

// 数据类用于查询结果
data class RecordStatistics(
    val total_count: Int,
    val oldest_timestamp: Long?,
    val newest_timestamp: Long?,
    val duplicate_count: Int
)

data class AverageData(
    val avg_temperature: Double,
    val avg_humidity: Double,
    val avg_light: Double,
    val avg_soil: Double
)

data class DailyCount(
    val date: String,
    val count: Int
)