package com.example.thebest.data.network

import android.content.Context
import android.util.Log
import com.example.thebest.data.model.SensorData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode

class ApiService(private val client: HttpClient, private val context: Context) {

    private val baseUrl: String by lazy {
        context.getString(context.resources.getIdentifier("api_base_url", "string", context.packageName))
    }

    private val apiType: String by lazy {
        context.getString(context.resources.getIdentifier("api_type", "string", context.packageName))
    }

    private val isCloudVersion: Boolean by lazy {
        baseUrl.contains("workers.dev") || baseUrl.contains("withgo.cn")
    }

    suspend fun getSensorData(): SensorData {
        val url = "$baseUrl/data"
        val response = client.get(url)
        return response.body<SensorData>()
    }

    suspend fun setThreshold(tempThreshold: Int, lightThreshold: Int): String {
        val url = "$baseUrl/setThreshold?tempThreshold=$tempThreshold&lightThreshold=$lightThreshold"
        val response = client.get(url)
        Log.d("network",response.toString())

        return if (response.status == HttpStatusCode.OK) {
            "设置成功"
        } else {
            throw Exception("HTTP ${response.status.value}")
        }
    }

    // 🧪 测试功能 - 只有云版本支持
    suspend fun triggerHighTempTest(): String {
        if (!isCloudVersion) {
            return "测试功能仅在云版本中可用"
        }

        client.get("$baseUrl/test/highTemp")
        return "高温测试触发成功"
    }

    suspend fun triggerLowLightTest(): String {
        if (!isCloudVersion) {
            return "测试功能仅在云版本中可用"
        }

        client.get("$baseUrl/test/lowLight")
        return "低光照测试触发成功"
    }

    suspend fun resetToNormal(): String {
        if (!isCloudVersion) {
            return "测试功能仅在云版本中可用"
        }

        client.get("$baseUrl/test/normal")
        return "数据已恢复正常"
    }

    // 获取当前配置信息
    fun getApiInfo(): Map<String, String> {
        return mapOf(
            "apiType" to apiType,
            "baseUrl" to baseUrl,
            "isCloudVersion" to isCloudVersion.toString()
        )
    }
}