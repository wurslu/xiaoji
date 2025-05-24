package com.example.thebest.service

import android.content.Context
import android.content.SharedPreferences
import com.example.thebest.data.model.SensorData
import com.example.thebest.notification.NotificationType
import com.example.thebest.notification.SensorNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SensorMonitorService(private val context: Context) {

    private val notificationManager = SensorNotificationManager(context)
    private val preferences: SharedPreferences =
        context.getSharedPreferences("sensor_thresholds", Context.MODE_PRIVATE)

    // 监控状态
    private val _monitoringState = MutableStateFlow(MonitoringState())
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    // 上次通知时间记录（避免频繁通知）
    private val lastNotificationTimes = mutableMapOf<NotificationType, Long>()
    private val notificationCooldown = 5 * 60 * 1000L // 5分钟冷却时间

    data class MonitoringState(
        val isMonitoringEnabled: Boolean = true,
        val temperatureThreshold: Int = 29,
        val lightThreshold: Int = 100,
        val quietHours: QuietHours = QuietHours(22, 7),
        val alertHistory: List<AlertRecord> = emptyList()
    )

    data class QuietHours(val startHour: Int, val endHour: Int)

    data class AlertRecord(
        val timestamp: Long,
        val type: NotificationType,
        val message: String,
        val value: String
    )

    init {
        loadSettings()
    }

    // 检查传感器数据并发送必要的通知
    fun checkSensorData(sensorData: SensorData) {
        if (!_monitoringState.value.isMonitoringEnabled || isQuietTime()) {
            return
        }

        val currentState = _monitoringState.value
        val alerts = mutableListOf<AlertRecord>()

        // 只检查温度过高
        checkTemperatureHigh(
            sensorData.temperature,
            currentState.temperatureThreshold
        )?.let { alert ->
            alerts.add(alert)
        }

        // 只检查光照不足
        checkLightLow(sensorData.light, currentState.lightThreshold)?.let { alert ->
            alerts.add(alert)
        }

        // 更新警报历史
        if (alerts.isNotEmpty()) {
            _monitoringState.value = currentState.copy(
                alertHistory = (currentState.alertHistory + alerts).takeLast(50)
            )
        }
    }

    // 检查温度过高
    private fun checkTemperatureHigh(temperature: Double, threshold: Int): AlertRecord? {
        return if (temperature > threshold) {
            if (shouldSendNotification(NotificationType.TEMPERATURE)) {
                notificationManager.sendTemperatureAlert(temperature, true)
                recordNotificationTime(NotificationType.TEMPERATURE)
            }
            AlertRecord(
                timestamp = System.currentTimeMillis(),
                type = NotificationType.TEMPERATURE,
                message = "温度过高",
                value = "${temperature}°C (阈值: ${threshold}°C)"
            )
        } else null
    }

    // 检查光照不足
    private fun checkLightLow(lightLevel: Int, threshold: Int): AlertRecord? {
        return if (lightLevel < threshold) {
            if (shouldSendNotification(NotificationType.LIGHT)) {
                notificationManager.sendLightAlert(lightLevel)
                recordNotificationTime(NotificationType.LIGHT)
            }
            AlertRecord(
                timestamp = System.currentTimeMillis(),
                type = NotificationType.LIGHT,
                message = "光照不足",
                value = "$lightLevel (阈值: $threshold)"
            )
        } else null
    }

    // 判断是否在静音时间
    private fun isQuietTime(): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val quietHours = _monitoringState.value.quietHours

        return if (quietHours.startHour > quietHours.endHour) {
            // 跨天的静音时间，如 22-7
            currentHour >= quietHours.startHour || currentHour < quietHours.endHour
        } else {
            // 同一天内的静音时间
            currentHour >= quietHours.startHour && currentHour < quietHours.endHour
        }
    }

    // 判断是否应该发送通知（避免频繁通知）
    private fun shouldSendNotification(type: NotificationType): Boolean {
        val lastTime = lastNotificationTimes[type] ?: 0
        return System.currentTimeMillis() - lastTime > notificationCooldown
    }

    // 记录通知发送时间
    private fun recordNotificationTime(type: NotificationType) {
        lastNotificationTimes[type] = System.currentTimeMillis()
    }

    // 更新阈值设置
    fun updateThresholds(tempThreshold: Int, lightThreshold: Int) {
        _monitoringState.value = _monitoringState.value.copy(
            temperatureThreshold = tempThreshold,
            lightThreshold = lightThreshold
        )
        saveSettings()
    }

    // 开启/关闭监控
    fun setMonitoringEnabled(enabled: Boolean) {
        _monitoringState.value = _monitoringState.value.copy(isMonitoringEnabled = enabled)
        saveSettings()
    }

    // 更新静音时段
    fun updateQuietHours(startHour: Int, endHour: Int) {
        _monitoringState.value = _monitoringState.value.copy(
            quietHours = QuietHours(startHour, endHour)
        )
        saveSettings()
    }

    // 保存设置
    private fun saveSettings() {
        val state = _monitoringState.value
        preferences.edit().apply {
            putBoolean("monitoring_enabled", state.isMonitoringEnabled)
            putInt("temp_threshold", state.temperatureThreshold)
            putInt("light_threshold", state.lightThreshold)
            putInt("quiet_start", state.quietHours.startHour)
            putInt("quiet_end", state.quietHours.endHour)
            apply()
        }
    }

    // 加载设置
    private fun loadSettings() {
        val monitoring = preferences.getBoolean("monitoring_enabled", true)
        val tempThreshold = preferences.getInt("temp_threshold", 29)
        val lightThreshold = preferences.getInt("light_threshold", 100)
        val quietStart = preferences.getInt("quiet_start", 22)
        val quietEnd = preferences.getInt("quiet_end", 7)

        _monitoringState.value = MonitoringState(
            isMonitoringEnabled = monitoring,
            temperatureThreshold = tempThreshold,
            lightThreshold = lightThreshold,
            quietHours = QuietHours(quietStart, quietEnd)
        )
    }

    // 清除警报历史
    fun clearAlertHistory() {
        _monitoringState.value = _monitoringState.value.copy(alertHistory = emptyList())
    }

    // 获取格式化的警报历史
    fun getFormattedAlertHistory(): List<String> {
        val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return _monitoringState.value.alertHistory.map { alert ->
            "${dateFormat.format(Date(alert.timestamp))} - ${alert.message}: ${alert.value}"
        }
    }

    // 获取当前阈值
    fun getCurrentThresholds(): Pair<Int, Int> {
        val state = _monitoringState.value
        return Pair(state.temperatureThreshold, state.lightThreshold)
    }
}