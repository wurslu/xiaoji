package com.example.thebest.data.network

import com.example.thebest.data.model.SensorData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode

class ApiService(private val client: HttpClient) {

//    private val baseUrl = "http://192.168.56.17:80"
    private val baseUrl = "http://10.0.2.2:3000"

    suspend fun getSensorData(): SensorData {
        val url = "$baseUrl/data"

        return try {
            val response = client.get(url)

            val data = response.body<SensorData>()
            data
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun setThreshold(tempThreshold: Int, lightThreshold: Int): String {
        val url =
            "$baseUrl/setThreshold?tempThreshold=$tempThreshold&lightThreshold=$lightThreshold"

        return try {
            val response = client.get(url)

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