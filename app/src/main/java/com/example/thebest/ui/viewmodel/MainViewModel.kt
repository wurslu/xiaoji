package com.example.thebest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.model.SensorData
import com.example.thebest.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(private val repository: SensorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 添加互斥锁防止并发请求
    private val requestMutex = Mutex()
    private var isRequestInProgress = false

    data class UiState(
        val sensorData: SensorData? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val lastUpdateTime: Long = 0L,
        val lastSaveTime: Long = 0L,
        val saveCount: Int = 0,
        val requestInterval: String = "10秒" // 新增：显示请求间隔
    )

    // 检查是否正在加载中（供外部调用）
    fun isCurrentlyLoading(): Boolean {
        return isRequestInProgress || _uiState.value.isLoading
    }

    // 首次获取数据（带保存）- 添加并发控制
    fun fetchSensorData() {
        viewModelScope.launch {
            // 使用互斥锁防止并发请求
            requestMutex.withLock {
                if (isRequestInProgress) {
                    return@withLock // 如果已有请求在进行，直接返回
                }

                isRequestInProgress = true
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

                try {
                    repository.getSensorData().collect { result ->
                        result.fold(
                            onSuccess = { data ->
                                val currentTime = System.currentTimeMillis()
                                _uiState.value = _uiState.value.copy(
                                    sensorData = data,
                                    isLoading = false,
                                    errorMessage = null,
                                    lastUpdateTime = currentTime,
                                    lastSaveTime = currentTime,
                                    saveCount = _uiState.value.saveCount + 1
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "未知错误"
                                )
                            }
                        )
                    }
                } finally {
                    isRequestInProgress = false
                }
            }
        }
    }

    // 仅刷新数据显示（不保存）- 添加并发控制和跳过机制
    fun refreshDataOnly() {
        viewModelScope.launch {
            // 快速检查：如果正在请求中，直接跳过
            if (isRequestInProgress) {
                println("跳过请求：上一个请求仍在进行中") // 调试用
                return@launch
            }

            requestMutex.withLock {
                // 双重检查：锁内再次确认没有请求在进行
                if (isRequestInProgress) {
                    return@withLock
                }

                isRequestInProgress = true

                // 只有在没有任何数据时才显示加载状态
                val shouldShowLoading = _uiState.value.sensorData == null
                if (shouldShowLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                try {
                    repository.getSensorDataOnly().collect { result ->
                        result.fold(
                            onSuccess = { data ->
                                _uiState.value = _uiState.value.copy(
                                    sensorData = data,
                                    isLoading = false,
                                    errorMessage = null,
                                    lastUpdateTime = System.currentTimeMillis()
                                )
                                println("数据更新成功：${System.currentTimeMillis()}") // 调试用
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = if (_uiState.value.sensorData == null) {
                                        error.message ?: "未知错误"
                                    } else null // 如果已有数据，不显示错误（静默失败）
                                )
                                println("数据更新失败：${error.message}") // 调试用
                            }
                        )
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
                    println("数据保存成功：第${_uiState.value.saveCount}次") // 调试用
                } catch (e: Exception) {
                    println("数据保存失败：${e.message}") // 调试用，不影响用户界面
                }
            }
        }
    }

    // 手动重试
    fun manualRetry() {
        // 重置状态后重新获取数据
        isRequestInProgress = false
        fetchSensorData()
    }

    // 兼容原有的静默刷新方法
    fun refreshDataSilently() {
        refreshDataOnly()
    }

    // 获取当前请求状态信息（用于调试）
    fun getRequestStatus(): String {
        return "请求状态: ${if (isRequestInProgress) "进行中" else "空闲"}, " +
                "UI加载: ${_uiState.value.isLoading}"
    }
}