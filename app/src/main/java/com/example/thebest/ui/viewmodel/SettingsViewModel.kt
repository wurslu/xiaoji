package com.example.thebest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val apiService: ApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val temperatureThreshold: Int = 29,
        val lightThreshold: Int = 100,
        val isSaving: Boolean = false,
        val saveMessage: String = "",
        val saveSuccess: Boolean = false
    )

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

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveMessage = "",
                saveSuccess = false
            )

            try {
                val result = apiService.setThreshold(
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
}