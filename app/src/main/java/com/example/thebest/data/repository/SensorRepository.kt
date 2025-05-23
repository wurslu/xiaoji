package com.example.thebest.data.repository


import com.example.thebest.data.model.SensorData
import com.example.thebest.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SensorRepository(private val apiService: ApiService) {

    fun getSensorData(): Flow<Result<SensorData>> = flow {
        try {
            val data = apiService.getSensorData()
            emit(Result.success(data))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}