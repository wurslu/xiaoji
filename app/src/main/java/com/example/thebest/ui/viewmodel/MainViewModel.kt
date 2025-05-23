package com.example.thebest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.model.SensorData
import com.example.thebest.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: SensorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val sensorData: SensorData? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val lastUpdateTime: Long = 0L
    )

    fun fetchSensorData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, errorMessage = null
            )

            repository.getSensorData().collect { result ->
                result.fold(onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        sensorData = data,
                        isLoading = false,
                        errorMessage = null,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }, onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, errorMessage = error.message ?: "未知错误"
                    )
                })
            }
        }
    }

    fun refreshDataSilently() {
        viewModelScope.launch {
            val shouldShowLoading = _uiState.value.sensorData == null

            if (shouldShowLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            repository.getSensorData().collect { result ->
                result.fold(onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        sensorData = data,
                        isLoading = false,
                        errorMessage = null,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }, onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (_uiState.value.sensorData == null) {
                            error.message ?: "未知错误"
                        } else null
                    )
                })
            }
        }
    }
}