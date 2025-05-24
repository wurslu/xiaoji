package com.example.thebest.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [SensorRecord::class],
    version = 1,
    exportSchema = false
)
abstract class SensorDatabase : RoomDatabase() {

    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANCE: SensorDatabase? = null

        fun getDatabase(context: Context): SensorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SensorDatabase::class.java,
                    "sensor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}