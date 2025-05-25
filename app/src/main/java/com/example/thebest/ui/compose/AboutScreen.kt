package com.example.thebest.ui.compose

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("关于应用") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用信息卡片
            item {
                AppInfoCard()
            }

            // 功能介绍
            item {
                FeatureCard()
            }

            // 技术信息
            item {
                TechnicalInfoCard()
            }

            // 版本信息
            item {
                VersionInfoCard()
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AppInfoCard() {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 使用 Material Design 图标，避免资源加载问题
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = "应用图标",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "The Best",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "环境数据监测系统",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "实时监测环境数据，智能预警，数据可视化分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun FeatureCard() {
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
            Text(
                text = "主要功能",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 直接写出来，避免循环可能的问题
            FeatureRow(Icons.Default.DeviceThermostat, "实时监测", "温度、湿度、光照、土壤湿度")
            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Default.Notifications, "智能通知", "环境异常自动提醒")
            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Default.History, "数据记录", "历史数据存储与查看")
            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Default.BarChart, "数据可视化", "图表展示数据趋势")
            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Default.Download, "数据导出", "CSV格式数据导出")
            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(Icons.Default.Widgets, "桌面小组件", "桌面实时显示数据")
        }
    }
}

@Composable
fun TechnicalInfoCard() {
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
            Text(
                text = "技术信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 简化显示，避免复杂的数据结构
            TechInfoRow(Icons.Default.Code, "开发框架", "Android Jetpack Compose")
            Spacer(modifier = Modifier.height(12.dp))

            TechInfoRow(Icons.Default.Storage, "数据库", "Room Database")
            Spacer(modifier = Modifier.height(12.dp))

            TechInfoRow(Icons.Default.Wifi, "网络请求", "Ktor Client")
            Spacer(modifier = Modifier.height(12.dp))

            TechInfoRow(Icons.Default.BarChart, "图表组件", "MPAndroidChart")
            Spacer(modifier = Modifier.height(12.dp))

            TechInfoRow(Icons.Default.Architecture, "架构模式", "MVVM + Repository")
            Spacer(modifier = Modifier.height(12.dp))

            TechInfoRow(Icons.Default.Palette, "UI设计", "Material Design 3")
        }
    }
}

@Composable
fun VersionInfoCard() {
    val context = LocalContext.current

    // 获取应用版本信息
    val (versionName, versionCode, appName) = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

            val appName = context.packageManager.getApplicationLabel(
                context.applicationInfo
            ).toString()

            Triple(
                packageInfo.versionName ?: "未知",
                packageInfo.longVersionCode.toString(),
                appName
            )
        } catch (e: Exception) {
            Triple("未知", "未知", "The Best")
        }
    }

    // 获取系统信息
    val (minSdkVersion, targetSdkVersion) = remember {
        try {
            val appInfo = context.applicationInfo
            Pair(appInfo.minSdkVersion, appInfo.targetSdkVersion)
        } catch (e: Exception) {
            Pair(24, 34)
        }
    }

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
            Text(
                text = "版本信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            InfoRow("应用名称", appName)
            InfoRow("版本号", versionName)
            InfoRow("构建版本", versionCode)
            InfoRow("发布日期", getCurrentDate())
            InfoRow(
                "最低系统要求",
                "Android ${getAndroidVersionName(minSdkVersion)} (API $minSdkVersion)"
            )
            InfoRow(
                "目标系统版本",
                "Android ${getAndroidVersionName(targetSdkVersion)} (API $targetSdkVersion)"
            )
            InfoRow(
                "当前设备系统",
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun TechInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 工具函数
private fun getCurrentDate(): String {
    return java.text.SimpleDateFormat("yyyy年MM月", java.util.Locale.getDefault())
        .format(java.util.Date())
}

private fun getAndroidVersionName(apiLevel: Int): String {
    return when (apiLevel) {
        34 -> "14"
        33 -> "13"
        32, 31 -> "12"
        30 -> "11"
        29 -> "10"
        28 -> "9"
        27, 26 -> "8"
        25, 24 -> "7"
        23 -> "6"
        22, 21 -> "5"
        else -> "${apiLevel / 10}.${apiLevel % 10}"
    }
}