package com.example.thebest.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // 首次获取数据（带保存）
        viewModel.fetchSensorData()

        launch {
            delay(2_000)
            while (true) {
                if (!viewModel.isCurrentlyLoading()) {
                    viewModel.refreshDataOnly()
                }
                delay(2_000)
            }
        }

        // 协程2：每分钟保存数据
        launch {
            delay(60_000) // 首次等待1分钟
            while (true) {
                delay(60_000) // 60秒 = 1分钟
                viewModel.saveCurrentData()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 简化的标题区域
            PageHeader(title = "环境数据监测")

            Spacer(modifier = Modifier.height(32.dp))

            when {
                uiState.isLoading && uiState.sensorData == null -> {
                    LoadingCard()
                }

                uiState.sensorData != null -> {
                    val data = uiState.sensorData!!

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SensorCard(
                            icon = Icons.Default.DeviceThermostat,
                            title = "温度",
                            value = "${data.temperature}°C",
                            color = MaterialTheme.colorScheme.error,
                            subtitle = getTemperatureStatus(data.temperature)
                        )

                        SensorCard(
                            icon = Icons.Default.WaterDrop,
                            title = "空气湿度",
                            value = "${data.humidity}%",
                            color = MaterialTheme.colorScheme.tertiary,
                            subtitle = getHumidityStatus(data.humidity)
                        )

                        SensorCard(
                            icon = Icons.Default.WbSunny,
                            title = "光照强度",
                            value = "${data.light}",
                            color = MaterialTheme.colorScheme.secondary,
                            subtitle = getLightStatus(data.light)
                        )

                        SensorCard(
                            icon = Icons.Default.Grass,
                            title = "土壤湿度",
                            value = "${data.soil}",
                            color = MaterialTheme.colorScheme.primary,
                            subtitle = getSoilStatus(data.soil)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 优化的状态指示器，显示请求间隔
                    OptimizedStatusIndicator(
                        isLoading = uiState.isLoading,
                        lastUpdateTime = uiState.lastUpdateTime,
                        lastSaveTime = uiState.lastSaveTime,
                        saveCount = uiState.saveCount
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }

                uiState.errorMessage != null -> {
                    ErrorCard(uiState.errorMessage!!) {
                        viewModel.fetchSensorData()
                    }
                }
            }
        }
    }
}

@Composable
fun PageHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun OptimizedStatusIndicator(
    isLoading: Boolean = false,
    lastUpdateTime: Long = 0L,
    lastSaveTime: Long = 0L,
    saveCount: Int = 0
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 如果正在加载，显示加载指示
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在更新数据...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (lastUpdateTime > 0L || lastSaveTime > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 数据更新时间
            if (lastUpdateTime > 0L) {
                val updateTime = remember(lastUpdateTime) {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(lastUpdateTime))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "数据更新: $updateTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 数据保存时间
            if (lastSaveTime > 0L) {
                if (lastUpdateTime > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val saveTime = remember(lastSaveTime) {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(lastSaveTime))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "数据保存: $saveTime (${saveCount}次)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SensorCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LoadingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
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
                modifier = Modifier
                    .size(48.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "正在获取传感器数据...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
fun ErrorCard(errorMessage: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "❌ 连接失败",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重新连接", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

fun getTemperatureStatus(temp: Double): String {
    return when {
        temp < 18 -> "偏冷"
        temp > 28 -> "偏热"
        else -> "适宜"
    }
}

fun getHumidityStatus(humidity: Double): String {
    return when {
        humidity < 30 -> "干燥"
        humidity > 70 -> "潮湿"
        else -> "适宜"
    }
}

fun getLightStatus(light: Int): String {
    return when {
        light < 200 -> "昏暗"
        light > 700 -> "强光"
        else -> "适中"
    }
}

fun getSoilStatus(soil: Int): String {
    return when (soil) {
        0 -> "干燥"
        1 -> "湿润"
        else -> "未知"
    }
}