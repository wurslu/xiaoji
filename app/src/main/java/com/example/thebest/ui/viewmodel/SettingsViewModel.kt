package com.example.thebest.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.network.ApiService
import com.example.thebest.service.SensorMonitorService
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

    data class UiState(
        val temperatureThreshold: Int = 29,
        val lightThreshold: Int = 100,
        val isSaving: Boolean = false,
        val saveMessage: String = "",
        val saveSuccess: Boolean = false,
        val isMonitoringEnabled: Boolean = true
    )

    // 暴露监控状态
    val monitoringState = monitorService?.monitoringState

    init {
        // 从监控服务加载当前阈值
        monitorService?.let { service ->
            val (tempThreshold, lightThreshold) = service.getCurrentThresholds()
            val isEnabled = service.monitoringState.value.isMonitoringEnabled

            _uiState.value = _uiState.value.copy(
                temperatureThreshold = tempThreshold,
                lightThreshold = lightThreshold,
                isMonitoringEnabled = isEnabled
            )
        }
    }

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
}