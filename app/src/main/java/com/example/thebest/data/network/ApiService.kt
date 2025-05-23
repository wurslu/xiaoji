package com.example.thebest.data.network

import com.example.thebest.data.model.SensorData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ApiService(private val client: HttpClient) {

    private val baseUrl = "http://192.168.78.156:80"

    suspend fun getSensorData(): SensorData {
        return client.get("$baseUrl/data").body()
    }
}