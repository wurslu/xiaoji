package com.example.thebest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebest.data.local.SensorRecord
import com.example.thebest.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: SensorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val records: List<SensorRecord> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val selectedDateRange: DateRange = DateRange.TODAY,
        val recordCount: Int = 0
    )

    enum class DateRange(val displayName: String) {
        TODAY("今天"),
        RECENT_24H("最近24小时"),
        RECENT_7D("最近7天"),
        ALL("全部")
    }

    init {
        // 启动实时监听
        startRealtimeUpdates()
    }

    private fun startRealtimeUpdates() {
        viewModelScope.launch {
            // 同时监听历史记录和记录总数的变化
            combine(
                repository.getHistoryRecords(), // 监听所有记录变化
                getSelectedRangeFlow() // 根据选择的时间范围获取对应数据
            ) { allRecords, filteredRecords ->
                Pair(allRecords.size, filteredRecords)
            }.collect { (totalCount, filteredRecords) ->
                _uiState.value = _uiState.value.copy(
                    records = filteredRecords,
                    recordCount = totalCount,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun getSelectedRangeFlow() = when (_uiState.value.selectedDateRange) {
        DateRange.TODAY -> repository.getTodayRecords()
        DateRange.RECENT_24H -> {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            repository.getRecordsByDateRange(oneDayAgo, System.currentTimeMillis())
        }

        DateRange.RECENT_7D -> {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            repository.getRecordsByDateRange(sevenDaysAgo, System.currentTimeMillis())
        }

        DateRange.ALL -> repository.getHistoryRecords()
    }

    fun selectDateRange(dateRange: DateRange) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = dateRange,
            isLoading = true
        )

        // 重新启动监听，使用新的时间范围
        startRealtimeUpdates()
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.clearAllRecords()
                // 清除后数据会通过Flow自动更新，不需要手动设置
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "清除数据失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}