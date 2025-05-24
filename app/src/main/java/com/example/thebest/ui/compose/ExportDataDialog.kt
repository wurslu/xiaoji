package com.example.thebest.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.thebest.data.local.SensorRecord
import com.example.thebest.utils.DataExportUtils
import kotlinx.coroutines.launch

@Composable
fun ExportDataDialog(
    records: List<SensorRecord>,
    onDismiss: () -> Unit,
    onExportComplete: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportSummary by remember { mutableStateOf<DataExportUtils.ExportSummary?>(null) }

    // 计算导出摘要
    LaunchedEffect(records) {
        exportSummary = DataExportUtils.getExportSummary(records)
    }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .width(340.dp) // 固定宽度，确保按钮有足够空间
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "导出数据",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 导出摘要
                exportSummary?.let { summary ->
                    ExportSummaryCard(summary)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (isExporting) {
                    // 导出中状态
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在导出数据...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "请稍候",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 导出选项
                    Column {
                        Text(
                            text = "导出格式：CSV (逗号分隔值)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "数据包含：时间、温度、湿度、光照强度、土壤湿度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 按钮行 - 使用更大的最小宽度确保水平排列
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 取消按钮
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "取消",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            // 导出按钮
                            Button(
                                onClick = {
                                    isExporting = true
                                    coroutineScope.launch {
                                        try {
                                            val result = DataExportUtils.exportToCSV(
                                                context = context,
                                                records = records,
                                                fileName = "sensor_data"
                                            )

                                            result.fold(
                                                onSuccess = { filePath ->
                                                    // 分享文件
                                                    DataExportUtils.shareExportedFile(
                                                        context,
                                                        filePath
                                                    )
                                                    onExportComplete(true, "数据导出成功！")
                                                },
                                                onFailure = { error ->
                                                    onExportComplete(
                                                        false,
                                                        "导出失败：${error.message}"
                                                    )
                                                }
                                            )
                                        } catch (e: Exception) {
                                            onExportComplete(false, "导出失败：${e.message}")
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "导出",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportSummaryCard(summary: DataExportUtils.ExportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "数据摘要",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 记录数和时间范围
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "记录数",
                    value = "${summary.totalRecords} 条",
                    icon = Icons.Default.DataUsage
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "时间范围：${summary.dateRange}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (summary.totalRecords > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                // 平均值
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "平均温度",
                        value = "${String.format("%.1f", summary.avgTemperature)}°C",
                        icon = Icons.Default.DeviceThermostat,
                        compact = true
                    )

                    SummaryItem(
                        label = "平均湿度",
                        value = "${String.format("%.1f", summary.avgHumidity)}%",
                        icon = Icons.Default.WaterDrop,
                        compact = true
                    )

                    SummaryItem(
                        label = "平均光照",
                        value = "${String.format("%.0f", summary.avgLight)}",
                        icon = Icons.Default.WbSunny,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean = false
) {
    if (compact) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}