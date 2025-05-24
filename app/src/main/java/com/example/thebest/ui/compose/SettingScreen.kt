package com.example.thebest.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部导航栏（仅在有返回回调时显示）
        if (onBack != null) {
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
                        text = "阈值设置",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 主要内容
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 如果没有顶部导航栏，显示简化标题
                if (onBack == null) {
                    PageHeader(title = "阈值设置")
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 设置卡片
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 温度阈值设置
                    TemperatureSettingCard(
                        tempThreshold = uiState.temperatureThreshold,
                        onTempThresholdChange = viewModel::updateTemperatureThreshold
                    )

                    // 光强阈值设置
                    LightSettingCard(
                        lightThreshold = uiState.lightThreshold,
                        onLightThresholdChange = viewModel::updateLightThreshold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 更新按钮
                    Button(
                        onClick = viewModel::saveSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        enabled = !uiState.isSaving
                    ) {
                        Text(
                            text = "更新阈值",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // 添加底部间距，确保按钮不被导航栏遮挡
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (uiState.isSaving) {
                LoadingOverlay()
            }

            if (uiState.saveMessage.isNotEmpty() && !uiState.isSaving) {
                ResultOverlay(
                    message = uiState.saveMessage,
                    isSuccess = uiState.saveSuccess,
                    onDismiss = viewModel::clearSaveMessage
                )
            }
        }
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "更新中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ResultOverlay(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message, isSuccess) {
        if (message.isNotEmpty()) {
            val delayTime = if (isSuccess) 1500L else 3000L
            kotlinx.coroutines.delay(delayTime)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 强制显示图标和状态信息
                Text(
                    text = if (isSuccess) "✅" else "❌",
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TemperatureSettingCard(
    tempThreshold: Int,
    onTempThresholdChange: (Int) -> Unit
) {
    SettingCard(
        icon = Icons.Default.DeviceThermostat,
        title = "温度阈值",
        color = MaterialTheme.colorScheme.error
    ) {
        SliderSetting(
            label = "大于设定阈值时，温控系统启动",
            value = tempThreshold,
            onValueChange = onTempThresholdChange,
            valueRange = 0..50,
            unit = "°C"
        )
    }
}

@Composable
fun LightSettingCard(
    lightThreshold: Int,
    onLightThresholdChange: (Int) -> Unit
) {
    SettingCard(
        icon = Icons.Default.WbSunny,
        title = "光强阈值",
        color = MaterialTheme.colorScheme.secondary
    ) {
        SliderSetting(
            label = "小于设定阈值时，补光系统启动",
            value = lightThreshold,
            onValueChange = onLightThresholdChange,
            valueRange = 0..1000,
            unit = ""
        )
    }
}

@Composable
fun SettingCard(
    icon: ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 内容
            content()
        }
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )

            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}