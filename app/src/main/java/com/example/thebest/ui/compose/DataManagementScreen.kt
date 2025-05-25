package com.example.thebest.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.DataManagementViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var clearType by remember { mutableStateOf(ClearDataType.ALL) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportRangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDataStatistics()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部导航栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 主要内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据统计概览
            item {
                DataStatisticsCard(statistics = uiState.dataStatistics)
            }

            // 数据导出
            item {
                Text(
                    text = "数据导出",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                ExportDataCard(
                    onExport = { showExportRangeDialog = true },
                    isExporting = uiState.isExporting
                )
            }

            // 数据清理
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "数据清理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                CleanupOptionsCard(
                    statistics = uiState.dataStatistics,
                    onCleanupSelected = { type ->
                        clearType = type
                        showClearDialog = true
                    },
                    isProcessing = uiState.isProcessing
                )
            }

            // 自动清理设置
            item {
                AutoCleanupCard(
                    isAutoCleanupEnabled = uiState.isAutoCleanupEnabled,
                    cleanupIntervalDays = uiState.autoCleanupDays,
                    onAutoCleanupToggle = viewModel::toggleAutoCleanup,
                    onIntervalChange = viewModel::updateAutoCleanupDays
                )
            }

            // 存储空间信息
            item {
                StorageInfoCard(
                    storageInfo = uiState.storageInfo
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 确认清理对话框
    if (showClearDialog) {
        ClearDataConfirmDialog(
            clearType = clearType,
            statistics = uiState.dataStatistics,
            onConfirm = {
                viewModel.clearData(clearType)
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    // 导出选择对话框
    if (showExportDialog) {
        ExportOptionsDialog(
            statistics = uiState.dataStatistics,
            onExportSelected = { exportType ->
                viewModel.exportData(exportType)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // 在现有的 showExportDialog 对话框后面添加
    if (showExportRangeDialog) {
        ExportRangeSelectionDialog(
            statistics = uiState.dataStatistics,
            onExportSelected = { exportType ->
                viewModel.exportData(exportType)
                showExportRangeDialog = false
            },
            onDismiss = { showExportRangeDialog = false }
        )
    }

    // 操作结果提示
    if (uiState.operationMessage.isNotEmpty()) {
        LaunchedEffect(uiState.operationMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearOperationMessage()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.operationSuccess)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.operationSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (uiState.operationSuccess)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.operationMessage,
                        color = if (uiState.operationSuccess)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DataStatisticsCard(statistics: DataStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "数据统计",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 只显示记录数和数据天数，删除占用空间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = Icons.Default.DataUsage,
                    label = "总记录数",
                    value = "${statistics.totalRecords}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                StatisticItem(
                    icon = Icons.Default.DateRange,
                    label = "数据天数",
                    value = "${statistics.daysCovered}天",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (statistics.oldestRecord != null && statistics.newestRecord != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "数据时间范围",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${statistics.oldestRecord} 至 ${statistics.newestRecord}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ExportDataCard(
    onExport: () -> Unit, // 改为单个回调
    isExporting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "导出数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "将传感器数据导出为CSV文件，方便在其他软件中分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 只保留一个导出按钮
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isExporting) "导出中..." else "选择导出范围")
            }
        }
    }
}

@Composable
fun ExportRangeSelectionDialog(
    statistics: DataStatistics,
    onExportSelected: (ExportType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "选择导出范围",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "请选择要导出的数据时间范围：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val exportOptions = listOf(
                    ExportOption(
                        type = ExportType.ALL,
                        title = "全部数据",
                        description = "导出所有历史记录 (${statistics.totalRecords}条)",
                        icon = Icons.Default.Storage,
                        recommended = false
                    ),
                    ExportOption(
                        type = ExportType.LAST_7_DAYS,
                        title = "最近7天",
                        description = "导出一周内的数据",
                        icon = Icons.Default.DateRange,
                        recommended = true
                    ),
                    ExportOption(
                        type = ExportType.LAST_30_DAYS,
                        title = "最近30天",
                        description = "导出一个月内的数据",
                        icon = Icons.Default.CalendarMonth,
                        recommended = false
                    ),
                    ExportOption(
                        type = ExportType.LAST_90_DAYS,
                        title = "最近90天",
                        description = "导出三个月内的数据",
                        icon = Icons.Default.CalendarViewMonth,
                        recommended = false
                    )
                )

                exportOptions.forEach { option ->
                    ExportOptionItem(
                        option = option,
                        onClick = { onExportSelected(option.type) }
                    )
                    if (option != exportOptions.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 修改 ExportOptionItem 组件，改进选中效果
@Composable
fun ExportOptionItem(
    option: ExportOption,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // 添加边框效果，只在推荐项显示
                if (option.recommended) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (option.recommended)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) // 减少背景透明度
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (option.recommended)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (option.recommended) FontWeight.Bold else FontWeight.Medium,
                        color = if (option.recommended)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    if (option.recommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (option.recommended)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CleanupOptionsCard(
    statistics: DataStatistics,
    onCleanupSelected: (ClearDataType) -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "数据清理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 只保留两个清理选项，删除重复数据清理
            val cleanupOptions = listOf(
                CleanupOption(
                    type = ClearDataType.OLD_DATA,
                    title = "清理旧数据",
                    description = "删除30天前的数据 (约${statistics.oldDataCount}条)",
                    icon = Icons.Default.History,
                    color = MaterialTheme.colorScheme.secondary
                ),
                CleanupOption(
                    type = ClearDataType.ALL,
                    title = "清空所有数据",
                    description = "删除全部历史记录 (${statistics.totalRecords}条)",
                    icon = Icons.Default.DeleteForever,
                    color = MaterialTheme.colorScheme.error
                )
            )

            cleanupOptions.forEach { option ->
                CleanupOptionItem(
                    option = option,
                    onSelected = { onCleanupSelected(option.type) },
                    enabled = !isProcessing
                )
                if (option != cleanupOptions.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AutoCleanupCard(
    isAutoCleanupEnabled: Boolean,
    cleanupIntervalDays: Int,
    onAutoCleanupToggle: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    var showIntervalDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动清理",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isAutoCleanupEnabled)
                            "每${cleanupIntervalDays}天自动清理旧数据"
                        else
                            "已关闭自动清理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Switch(
                    checked = isAutoCleanupEnabled,
                    onCheckedChange = onAutoCleanupToggle
                )
            }

            if (isAutoCleanupEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showIntervalDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("调整清理间隔 (${cleanupIntervalDays}天)")
                }
            }
        }
    }

    if (showIntervalDialog) {
        CleanupIntervalDialog(
            currentInterval = cleanupIntervalDays,
            onIntervalSelected = { interval ->
                onIntervalChange(interval)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
}

@Composable
fun StorageInfoCard(storageInfo: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "存储空间信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            StorageInfoItem("数据库大小", formatFileSize(storageInfo.databaseSize))
            StorageInfoItem("导出文件", formatFileSize(storageInfo.exportFilesSize))
            StorageInfoItem("缓存文件", formatFileSize(storageInfo.cacheSize))

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            StorageInfoItem(
                "总占用空间",
                formatFileSize(storageInfo.totalSize),
                isTotal = true
            )
        }
    }
}

@Composable
fun StatisticItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CleanupOptionItem(
    option: CleanupOption,
    onSelected: () -> Unit,
    enabled: Boolean
) {
    Card(
        onClick = {
            if (enabled) {
                onSelected()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = option.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StorageInfoItem(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ClearDataConfirmDialog(
    clearType: ClearDataType,
    statistics: DataStatistics,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message, recordCount) = when (clearType) {
        ClearDataType.OLD_DATA -> Triple(
            "清理旧数据",
            "确定要删除30天前的数据吗？此操作不可恢复。",
            statistics.oldDataCount
        )

        ClearDataType.ALL -> Triple(
            "清空所有数据",
            "确定要删除全部历史记录吗？此操作不可恢复！",
            statistics.totalRecords
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "将删除 $recordCount 条记录",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ExportOptionsDialog(
    statistics: DataStatistics,
    onExportSelected: (ExportType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择导出范围",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                val exportOptions = listOf(
                    ExportType.ALL to "全部数据 (${statistics.totalRecords}条)",
                    ExportType.LAST_7_DAYS to "最近7天",
                    ExportType.LAST_30_DAYS to "最近30天",
                    ExportType.LAST_90_DAYS to "最近90天"
                )

                exportOptions.forEach { (type, description) ->
                    TextButton(
                        onClick = { onExportSelected(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = description,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CleanupIntervalDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(7, 14, 30, 60, 90, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自动清理间隔",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "选择自动清理的时间间隔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                intervals.forEach { interval ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = interval == currentInterval,
                            onClick = { onIntervalSelected(interval) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "${interval}天",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

// 工具函数
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"

    val df = DecimalFormat("#.##")
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "${df.format(size)} ${units[unitIndex]}"
}

data class DataStatistics(
    val totalRecords: Int = 0,
    val daysCovered: Int = 0,
    val oldDataCount: Int = 0,
    val oldestRecord: String? = null,
    val newestRecord: String? = null
)

data class StorageInfo(
    val databaseSize: Long = 0L,
    val exportFilesSize: Long = 0L,
    val cacheSize: Long = 0L,
    val totalSize: Long = 0L
)

data class CleanupOption(
    val type: ClearDataType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

enum class ClearDataType {
    OLD_DATA,
    ALL
}

enum class ExportType {
    ALL,           // 导出全部
    LAST_7_DAYS,   // 最近7天
    LAST_30_DAYS,  // 最近30天
    LAST_90_DAYS   // 最近90天
}

data class ExportOption(
    val type: ExportType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val recommended: Boolean = false
)