package com.example.thebest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "sensor_records")
@Serializable
data class SensorRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperature: Double,
    val humidity: Double,
    val light: Int,
    val soil: Int,
    val timestamp: Long = System.currentTimeMillis()
)