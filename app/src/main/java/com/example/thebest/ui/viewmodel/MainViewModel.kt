package com.example.thebest.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.model.SensorData
import com.example.thebest.data.repository.SensorRepository
import com.example.thebest.service.SensorMonitorService
import com.example.thebest.widget.SensorWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(
    private val repository: SensorRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 传感器监控服务
    private val monitorService = context?.let {
        Log.d("MainViewModel", "创建监控服务，Context: $it")
        SensorMonitorService(it)
    }.also {
        Log.d("MainViewModel", "监控服务创建结果: $it")
    }

    // 添加互斥锁防止并发请求
    private val requestMutex = Mutex()
    private var isRequestInProgress = false

    // 定时刷新和自动保存的Job
    private var refreshJob: Job? = null
    private var autoSaveJob: Job? = null

    data class UiState(
        val sensorData: SensorData? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val lastUpdateTime: Long = 0L,
        val lastSaveTime: Long = 0L,
        val saveCount: Int = 0,
        val requestInterval: String = "2秒",
        val hasActiveAlerts: Boolean = false,
        val alertCount: Int = 0
    )

    // 暴露监控状态
    val monitoringState = monitorService?.monitoringState

    init {
        // 启动定时任务
        startPeriodicTasks()
    }

    // 检查是否正在加载中（供外部调用）
    fun isCurrentlyLoading(): Boolean {
        return isRequestInProgress || _uiState.value.isLoading
    }

    private fun startPeriodicTasks() {
        context?.let { ctx ->
            // 获取当前设置
            val updateFrequency = SettingsViewModel.getUpdateFrequency(ctx)
            val (autoSaveEnabled, autoSaveInterval) = SettingsViewModel.getAutoSaveSettings(ctx)

            // 更新UI显示的间隔信息
            _uiState.value = _uiState.value.copy(
                requestInterval = updateFrequency.displayName
            )

            // 启动数据刷新任务
            startRefreshTask(updateFrequency.intervalMs)

            // 启动自动保存任务
            if (autoSaveEnabled) {
                startAutoSaveTask(autoSaveInterval * 60 * 1000L) // 转换为毫秒
            }
        }
    }

    private fun startRefreshTask(intervalMs: Long) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            // 首次获取数据（带保存）
            fetchSensorData()

            // 等待首次加载完成
            delay(3000)

            // 定时刷新（仅更新显示）
            while (true) {
                if (!isCurrentlyLoading()) {
                    refreshDataOnly()
                }
                delay(intervalMs)
            }
        }
    }

    private fun startAutoSaveTask(intervalMs: Long) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(intervalMs) // 首次等待
            while (true) {
                saveCurrentData()
                delay(intervalMs)
            }
        }
    }

    // 重新启动定时任务（当设置更改时调用）
    fun restartPeriodicTasks() {
        startPeriodicTasks()
    }

    // 首次获取数据（带保存）- 添加监控检查
    fun fetchSensorData() {
        viewModelScope.launch {
            requestMutex.withLock {
                if (isRequestInProgress) {
                    return@withLock
                }

                isRequestInProgress = true
                _uiState.value = _uiState.value.copy(
                    isLoading = true, errorMessage = null
                )

                try {
                    repository.getSensorData().collect { result ->
                        result.fold(onSuccess = { data ->
                            val currentTime = System.currentTimeMillis()

                            Log.d(
                                "MainViewModel",
                                "收到传感器数据: 温度=${data.temperature}, 光照=${data.light}"
                            )

                            // 检查数据异常并发送通知
                            monitorService?.let {
                                Log.d("MainViewModel", "调用监控服务检查数据")
                                it.checkSensorData(data)
                            } ?: Log.w("MainViewModel", "监控服务为null，无法检查数据")

                            // 更新警报状态
                            val alertHistory = monitorService?.monitoringState?.value?.alertHistory
                                ?: emptyList()
                            val recentAlertsCount = alertHistory.count {
                                currentTime - it.timestamp < 60 * 60 * 1000L // 最近1小时的警报
                            }

                            _uiState.value = _uiState.value.copy(
                                sensorData = data,
                                isLoading = false,
                                errorMessage = null,
                                lastUpdateTime = currentTime,
                                lastSaveTime = currentTime,
                                saveCount = _uiState.value.saveCount + 1,
                                hasActiveAlerts = recentAlertsCount > 0,
                                alertCount = recentAlertsCount
                            )

                            // 更新Widget数据
                            context?.let {
                                SensorWidgetProvider.updateWidgetData(
                                    it, data.temperature, data.humidity, data.light, data.soil
                                )
                            }
                        }, onFailure = { error ->
                            val detailedError = when {
                                error.message?.contains("Connection refused") == true -> "服务器连接被拒绝 - 请检查服务器是否启动"

                                error.message?.contains("timeout") == true -> "连接超时 - 请检查网络连接"

                                error.message?.contains("UnknownHostException") == true -> "无法解析主机 - 请检查IP地址"

                                else -> "网络错误: ${error.message}"
                            }

                            _uiState.value = _uiState.value.copy(
                                isLoading = false, errorMessage = detailedError
                            )
                        })
                    }
                } finally {
                    isRequestInProgress = false
                }
            }
        }
    }

    // 仅刷新数据显示（不保存）- 添加监控检查
    fun refreshDataOnly() {
        viewModelScope.launch {
            if (isRequestInProgress) {
                return@launch
            }

            requestMutex.withLock {
                if (isRequestInProgress) {
                    return@withLock
                }

                isRequestInProgress = true
                val shouldShowLoading = _uiState.value.sensorData == null
                if (shouldShowLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                try {
                    repository.getSensorDataOnly().collect { result ->
                        result.fold(onSuccess = { data ->
                            val currentTime = System.currentTimeMillis()

                            // 检查数据异常
                            monitorService?.checkSensorData(data)

                            // 更新警报状态
                            val alertHistory = monitorService?.monitoringState?.value?.alertHistory
                                ?: emptyList()
                            val recentAlertsCount = alertHistory.count {
                                currentTime - it.timestamp < 60 * 60 * 1000L
                            }

                            _uiState.value = _uiState.value.copy(
                                sensorData = data,
                                isLoading = false,
                                errorMessage = null,
                                lastUpdateTime = currentTime,
                                hasActiveAlerts = recentAlertsCount > 0,
                                alertCount = recentAlertsCount
                            )

                            // 更新Widget数据
                            context?.let {
                                SensorWidgetProvider.updateWidgetData(
                                    it, data.temperature, data.humidity, data.light, data.soil
                                )
                            }
                        }, onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = if (_uiState.value.sensorData == null) {
                                    error.message ?: "未知错误"
                                } else null
                            )
                        })
                    }
                } finally {
                    isRequestInProgress = false
                }
            }
        }
    }

    // 保存当前数据到本地
    fun saveCurrentData() {
        viewModelScope.launch {
            _uiState.value.sensorData?.let { data ->
                try {
                    repository.saveSensorData(data)
                    _uiState.value = _uiState.value.copy(
                        lastSaveTime = System.currentTimeMillis(),
                        saveCount = _uiState.value.saveCount + 1
                    )
                } catch (e: Exception) {
                    // 忽略保存错误，不影响实时显示
                    Log.w("MainViewModel", "保存数据失败: ${e.message}")
                }
            }
        }
    }

    // 手动重试
    fun manualRetry() {
        isRequestInProgress = false
        fetchSensorData()
    }

    // 兼容原有的静默刷新方法
    fun refreshDataSilently() {
        refreshDataOnly()
    }

    // 获取警报历史
    fun getAlertHistory(): List<String> {
        return monitorService?.getFormattedAlertHistory() ?: emptyList()
    }

    // 清除警报历史
    fun clearAlertHistory() {
        monitorService?.clearAlertHistory()
        _uiState.value = _uiState.value.copy(
            hasActiveAlerts = false, alertCount = 0
        )
    }

    // 更新监控设置
    fun updateMonitoringSettings(tempThreshold: Int, lightThreshold: Int) {
        monitorService?.updateThresholds(tempThreshold, lightThreshold)
    }

    override fun onCleared() {
        super.onCleared()
        // 取消所有定时任务
        refreshJob?.cancel()
        autoSaveJob?.cancel()
    }
}