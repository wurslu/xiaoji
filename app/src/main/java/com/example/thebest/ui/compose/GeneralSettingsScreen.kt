package com.example.thebest.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    text = "通用设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 设置内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据更新设置
            item {
                Text(
                    text = "数据更新",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                UpdateFrequencyCard(
                    currentFrequency = uiState.updateFrequency,
                    onFrequencyChange = viewModel::updateFrequency
                )
            }

            // 主题设置
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "主题设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                DynamicThemeCard(
                    isDynamicThemeEnabled = uiState.isDynamicThemeEnabled,
                    onToggle = viewModel::toggleDynamicTheme
                )
            }

            // 其他设置
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "其他设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                AutoSaveCard(
                    isAutoSaveEnabled = uiState.isAutoSaveEnabled,
                    autoSaveInterval = uiState.autoSaveInterval,
                    onAutoSaveToggle = viewModel::toggleAutoSave,
                    onIntervalChange = viewModel::updateAutoSaveInterval
                )
            }

            item {
                DataRetentionCard(
                    retentionDays = uiState.dataRetentionDays,
                    onRetentionChange = viewModel::updateDataRetention
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 保存结果提示
    if (uiState.saveMessage.isNotEmpty()) {
        LaunchedEffect(uiState.saveMessage) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveMessage()
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
                    containerColor = if (uiState.saveSuccess)
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
                        imageVector = if (uiState.saveSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (uiState.saveSuccess)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.saveMessage,
                        color = if (uiState.saveSuccess)
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
fun UpdateFrequencyCard(
    currentFrequency: UpdateFrequency,
    onFrequencyChange: (UpdateFrequency) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    GeneralSettingCard(
        icon = Icons.Default.Refresh,
        title = "数据更新频率",
        subtitle = currentFrequency.displayName,
        iconColor = MaterialTheme.colorScheme.primary,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        UpdateFrequencyDialog(
            currentFrequency = currentFrequency,
            onFrequencySelected = { frequency ->
                onFrequencyChange(frequency)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun DynamicThemeCard(
    isDynamicThemeEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    GeneralSettingToggleCard(
        icon = Icons.Default.Palette,
        title = "动态主题",
        subtitle = if (isDynamicThemeEnabled) "跟随系统颜色" else "使用应用默认主题",
        iconColor = MaterialTheme.colorScheme.secondary,
        isEnabled = isDynamicThemeEnabled,
        onToggle = onToggle
    )
}

@Composable
fun AutoSaveCard(
    isAutoSaveEnabled: Boolean,
    autoSaveInterval: Int,
    onAutoSaveToggle: (Boolean) -> Unit,
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
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动保存",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isAutoSaveEnabled) "每${autoSaveInterval}分钟自动保存数据" else "已关闭自动保存",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Switch(
                    checked = isAutoSaveEnabled,
                    onCheckedChange = onAutoSaveToggle
                )
            }

            if (isAutoSaveEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showIntervalDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("调整保存间隔 (${autoSaveInterval}分钟)")
                }
            }
        }
    }

    if (showIntervalDialog) {
        SaveIntervalDialog(
            currentInterval = autoSaveInterval,
            onIntervalSelected = { interval ->
                onIntervalChange(interval)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
}

@Composable
fun DataRetentionCard(
    retentionDays: Int,
    onRetentionChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    GeneralSettingCard(
        icon = Icons.Default.Storage,
        title = "数据保留时间",
        subtitle = "${retentionDays}天后自动清理历史数据",
        iconColor = MaterialTheme.colorScheme.error,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        DataRetentionDialog(
            currentDays = retentionDays,
            onDaysSelected = { days ->
                onRetentionChange(days)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun GeneralSettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GeneralSettingToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

// 对话框组件
@Composable
fun UpdateFrequencyDialog(
    currentFrequency: UpdateFrequency,
    onFrequencySelected: (UpdateFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择更新频率",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "设置传感器数据的刷新频率",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                UpdateFrequency.values().forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = frequency == currentFrequency,
                            onClick = { onFrequencySelected(frequency) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = frequency.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = frequency.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

@Composable
fun SaveIntervalDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(1, 2, 5, 10, 15, 30, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自动保存间隔",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "选择自动保存数据的时间间隔",
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
                            text = "${interval}分钟",
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

@Composable
fun DataRetentionDialog(
    currentDays: Int,
    onDaysSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val retentionOptions = listOf(7, 14, 30, 60, 90, 180, 365)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "数据保留时间",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "超过设定时间的历史数据将被自动清理",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                retentionOptions.forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = days == currentDays,
                            onClick = { onDaysSelected(days) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = when (days) {
                                7 -> "7天 (1周)"
                                14 -> "14天 (2周)"
                                30 -> "30天 (1个月)"
                                60 -> "60天 (2个月)"
                                90 -> "90天 (3个月)"
                                180 -> "180天 (6个月)"
                                365 -> "365天 (1年)"
                                else -> "${days}天"
                            },
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

// 枚举类
enum class UpdateFrequency(
    val displayName: String,
    val description: String,
    val intervalMs: Long
) {
    FAST("快速 (1秒)", "实时性最好，耗电较多", 1000L),
    NORMAL("正常 (2秒)", "平衡实时性和耗电 (推荐)", 2000L),
    SLOW("节能 (5秒)", "节省电量，适合长时间监测", 5000L),
    VERY_SLOW("超节能 (10秒)", "最省电，适合演示", 10000L)
}