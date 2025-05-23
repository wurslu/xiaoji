package com.example.thebest.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 自动获取数据
    LaunchedEffect(Unit) {
        // 第一次获取数据
        viewModel.fetchSensorData()
        // 每5秒静默刷新一次
        while (true) {
            delay(5000)
            viewModel.refreshDataSilently()
        }
    }

    // 渐变背景
    val gradientColors = listOf(
        Color(0xFF667eea),
        Color(0xFF764ba2),
        Color(0xFF6B73FF)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(gradientColors)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题区域
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "智能传感器监控",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "实时环境数据监测",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            when {
                uiState.isLoading && uiState.sensorData == null -> {
                    // 只在第一次加载或没有数据时显示加载界面
                    LoadingCard()
                }

                uiState.sensorData != null -> {
                    val data = uiState.sensorData!!

                    // 传感器数据卡片
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SensorCard(
                            icon = Icons.Default.DeviceThermostat,
                            title = "温度",
                            value = "${data.temperature}°C",
                            color = Color(0xFFFF6B6B),
                            subtitle = getTemperatureStatus(data.temperature)
                        )

                        SensorCard(
                            icon = Icons.Default.WaterDrop,
                            title = "湿度",
                            value = "${data.humidity}%",
                            color = Color(0xFF4ECDC4),
                            subtitle = getHumidityStatus(data.humidity)
                        )

                        SensorCard(
                            icon = Icons.Default.WbSunny,
                            title = "光照强度",
                            value = "${data.light}",
                            color = Color(0xFFFFE66D),
                            subtitle = getLightStatus(data.light)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 状态指示器 - 显示更新状态
                    StatusIndicator(
                        isUpdating = uiState.isLoading,
                        lastUpdateTime = uiState.lastUpdateTime
                    )
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
fun SensorCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标区域
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

            // 文字区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
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
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF667eea),
                modifier = Modifier
                    .size(48.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "正在获取传感器数据...",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
fun ErrorCard(errorMessage: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
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
                color = Color(0xFFFF6B6B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重新连接", color = Color.White)
            }
        }
    }
}

@Composable
fun StatusIndicator(
    isUpdating: Boolean = false,
    lastUpdateTime: Long = 0L
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUpdating) 1.2f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isUpdating) 500 else 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val statusColor = if (isUpdating) Color(0xFFFF9800) else Color(0xFF4CAF50)
    val statusText = if (isUpdating) "正在更新..." else "实时监控中"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp * scale)
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (lastUpdateTime > 0L && !isUpdating) {
                val updateTime = remember(lastUpdateTime) {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(lastUpdateTime))
                }
                Text(
                    text = "更新时间: $updateTime",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// 辅助函数
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
        light > 1000 -> "强光"
        else -> "适中"
    }
}