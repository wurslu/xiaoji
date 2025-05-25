package com.example.thebest.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.network.ApiService
import com.example.thebest.service.SensorMonitorService
import com.example.thebest.ui.compose.UpdateFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiService: ApiService,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 监控服务
    private val monitorService = context?.let { SensorMonitorService(it) }

    // SharedPreferences for general settings
    private val generalPrefs: SharedPreferences? = context?.getSharedPreferences(
        "general_settings", Context.MODE_PRIVATE
    )

    data class UiState(
        // 阈值设置
        val temperatureThreshold: Int = 29,
        val lightThreshold: Int = 100,
        val isSaving: Boolean = false,
        val saveMessage: String = "",
        val saveSuccess: Boolean = false,
        val isMonitoringEnabled: Boolean = true,

        // 通用设置
        val updateFrequency: UpdateFrequency = UpdateFrequency.NORMAL,
        val isDynamicThemeEnabled: Boolean = true,
        val isAutoSaveEnabled: Boolean = true,
        val autoSaveInterval: Int = 1, // 分钟
        val dataRetentionDays: Int = 30
    )

    // 暴露监控状态
    val monitoringState = monitorService?.monitoringState

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        // 从监控服务加载阈值设置
        monitorService?.let { service ->
            val (tempThreshold, lightThreshold) = service.getCurrentThresholds()
            val isEnabled = service.monitoringState.value.isMonitoringEnabled

            _uiState.value = _uiState.value.copy(
                temperatureThreshold = tempThreshold,
                lightThreshold = lightThreshold,
                isMonitoringEnabled = isEnabled
            )
        }

        // 从SharedPreferences加载通用设置
        generalPrefs?.let { prefs ->
            val frequencyOrdinal = prefs.getInt("update_frequency", UpdateFrequency.NORMAL.ordinal)
            val updateFrequency =
                UpdateFrequency.entries.toTypedArray().getOrElse(frequencyOrdinal) { UpdateFrequency.NORMAL }

            _uiState.value = _uiState.value.copy(
                updateFrequency = updateFrequency,
                isDynamicThemeEnabled = prefs.getBoolean("dynamic_theme", true),
                isAutoSaveEnabled = prefs.getBoolean("auto_save", true),
                autoSaveInterval = prefs.getInt("auto_save_interval", 1),
                dataRetentionDays = prefs.getInt("data_retention_days", 30)
            )
        }
    }

    // 阈值设置方法
    fun updateTemperatureThreshold(value: Int) {
        _uiState.value = _uiState.value.copy(
            temperatureThreshold = value,
            saveMessage = ""
        )
    }

    fun updateLightThreshold(value: Int) {
        _uiState.value = _uiState.value.copy(
            lightThreshold = value,
            saveMessage = ""
        )
    }

    fun updateMonitoringEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isMonitoringEnabled = enabled,
            saveMessage = ""
        )

        // 立即更新监控服务状态
        monitorService?.setMonitoringEnabled(enabled)
    }

    fun updateQuietHours(startHour: Int, endHour: Int) {
        monitorService?.updateQuietHours(startHour, endHour)
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveMessage = "",
                saveSuccess = false
            )

            try {
                // 1. 保存到服务器
                val result = apiService.setThreshold(
                    tempThreshold = _uiState.value.temperatureThreshold,
                    lightThreshold = _uiState.value.lightThreshold
                )

                // 2. 更新本地监控服务的阈值
                monitorService?.updateThresholds(
                    tempThreshold = _uiState.value.temperatureThreshold,
                    lightThreshold = _uiState.value.lightThreshold
                )

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveMessage = result,
                    saveSuccess = true
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveMessage = "设置失败: ${e.message}",
                    saveSuccess = false
                )
            }
        }
    }

    // 通用设置方法
    fun updateFrequency(frequency: UpdateFrequency) {
        _uiState.value = _uiState.value.copy(updateFrequency = frequency)
        saveGeneralSettings()

        // 这里可以添加逻辑来通知MainActivity更新数据刷新频率
        // 可以通过事件总线或其他方式实现
    }

    fun toggleDynamicTheme(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDynamicThemeEnabled = enabled)
        saveGeneralSettings()

        // 显示重启提示
        _uiState.value = _uiState.value.copy(
            saveMessage = "主题设置已保存，重启应用后生效",
            saveSuccess = true
        )
    }

    fun toggleAutoSave(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoSaveEnabled = enabled)
        saveGeneralSettings()
    }

    fun updateAutoSaveInterval(interval: Int) {
        _uiState.value = _uiState.value.copy(autoSaveInterval = interval)
        saveGeneralSettings()
    }

    fun updateDataRetention(days: Int) {
        _uiState.value = _uiState.value.copy(dataRetentionDays = days)
        saveGeneralSettings()

        // 立即执行数据清理
        viewModelScope.launch {
            try {
                // 这里可以调用repository的清理方法
                // repository.cleanOldRecords(days)
                _uiState.value = _uiState.value.copy(
                    saveMessage = "数据保留设置已更新",
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveMessage = "设置更新失败: ${e.message}",
                    saveSuccess = false
                )
            }
        }
    }

    private fun saveGeneralSettings() {
        generalPrefs?.edit()?.apply {
            putInt("update_frequency", _uiState.value.updateFrequency.ordinal)
            putBoolean("dynamic_theme", _uiState.value.isDynamicThemeEnabled)
            putBoolean("auto_save", _uiState.value.isAutoSaveEnabled)
            putInt("auto_save_interval", _uiState.value.autoSaveInterval)
            putInt("data_retention_days", _uiState.value.dataRetentionDays)
            apply()
        }
    }

    fun clearSaveMessage() {
        _uiState.value = _uiState.value.copy(
            saveMessage = "",
            saveSuccess = false
        )
    }

    // 获取警报历史
    fun getAlertHistory(): List<String> {
        return monitorService?.getFormattedAlertHistory() ?: emptyList()
    }

    // 清除警报历史
    fun clearAlertHistory() {
        monitorService?.clearAlertHistory()
    }

    companion object {
        // 用于其他组件获取设置的静态方法
        fun getUpdateFrequency(context: Context): UpdateFrequency {
            val prefs = context.getSharedPreferences("general_settings", Context.MODE_PRIVATE)
            val frequencyOrdinal = prefs.getInt("update_frequency", UpdateFrequency.NORMAL.ordinal)
            return UpdateFrequency.entries.toTypedArray().getOrElse(frequencyOrdinal) { UpdateFrequency.NORMAL }
        }

        fun isDynamicThemeEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("general_settings", Context.MODE_PRIVATE)
            return prefs.getBoolean("dynamic_theme", true)
        }

        fun getAutoSaveSettings(context: Context): Pair<Boolean, Int> {
            val prefs = context.getSharedPreferences("general_settings", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("auto_save", true)
            val interval = prefs.getInt("auto_save_interval", 1)
            return Pair(enabled, interval)
        }
    }

    fun updateNotificationCooldown(minutes: Int) {
        monitorService?.updateNotificationCooldown(minutes)
    }
}