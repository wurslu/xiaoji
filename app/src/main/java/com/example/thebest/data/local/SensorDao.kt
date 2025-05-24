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
}