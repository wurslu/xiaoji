package com.example.thebest.data.network

import com.example.thebest.data.model.SensorData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode

class ApiService(private val client: HttpClient) {

    private val port = "80"
    private val baseUrl = "http://192.168.78.156:$port"

    suspend fun getSensorData(): SensorData {
        return client.get("$baseUrl/data").body()
    }

    suspend fun setThreshold(tempThreshold: Int, lightThreshold: Int): String {
        return try {
            val response = client.get("$baseUrl/setThreshold?tempThreshold=$tempThreshold&lightThreshold=$lightThreshold")

            if (response.status == HttpStatusCode.OK) {
                "设置成功"
            } else {
                throw Exception("HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}