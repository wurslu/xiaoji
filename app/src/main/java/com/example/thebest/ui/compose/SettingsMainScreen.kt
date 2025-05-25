package com.example.thebest.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thebest.ui.viewmodel.SettingsViewModel
import com.example.thebest.utils.NotificationPermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel,
    onNavigateToThresholds: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val monitoringState by viewModel.monitoringState?.collectAsStateWithLifecycle() ?: remember {
        mutableStateOf(null)
    }

    // 实时检查通知权限状态
    val context = LocalContext.current
    val hasNotificationPermission by remember {
        derivedStateOf { NotificationPermissionManager.hasPermission(context) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageHeader(title = "设置")
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "监控设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(monitoringSettings) { setting ->
            SettingCard(
                setting = setting,
                onClick = {
                    when (setting.key) {
                        "thresholds" -> onNavigateToThresholds()
                        "notifications" -> onNavigateToNotifications()
                        else -> {}
                    }
                },
                subtitle = when (setting.key) {
                    "thresholds" -> "温度 ${uiState.temperatureThreshold}°C • 光照 ${uiState.lightThreshold}"
                    "notifications" -> {
                        when {
                            !hasNotificationPermission -> "需要权限"
                            monitoringState?.isMonitoringEnabled == true -> "已开启"
                            else -> "已关闭"
                        }
                    }

                    else -> setting.subtitle
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "通用设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(generalSettings) { setting ->
            SettingCard(
                setting = setting,
                onClick = {
                    when (setting.key) {
                        "general" -> onNavigateToGeneral()
                        "data" -> onNavigateToDataManagement()
                        "about" -> onNavigateToAbout()
                        else -> {}
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingCard(
    setting: SettingItem,
    onClick: () -> Unit,
    subtitle: String = setting.subtitle
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
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(setting.iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = setting.icon,
                    contentDescription = setting.title,
                    tint = setting.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class SettingItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: androidx.compose.ui.graphics.Color
)

val monitoringSettings = listOf(
    SettingItem(
        key = "thresholds",
        title = "阈值设置",
        subtitle = "设置温度和光照阈值",
        icon = Icons.Default.Tune,
        iconColor = androidx.compose.ui.graphics.Color(0xFF6200EE)
    ),
    SettingItem(
        key = "notifications",
        title = "通知设置",
        subtitle = "管理通知和静音时段",
        icon = Icons.Default.Notifications,
        iconColor = androidx.compose.ui.graphics.Color(0xFF03DAC5)
    )
)

val generalSettings = listOf(
    SettingItem(
        key = "general",
        title = "通用设置",
        subtitle = "数据更新频率、主题等",
        icon = Icons.Default.Settings,
        iconColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)
    ),
    SettingItem(
        key = "data",
        title = "数据管理",
        subtitle = "导出、清理历史数据",
        icon = Icons.Default.Storage,
        iconColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
    ),
    SettingItem(
        key = "about",
        title = "关于应用",
        subtitle = "版本信息和帮助",
        icon = Icons.Default.Info,
        iconColor = androidx.compose.ui.graphics.Color(0xFF607D8B)
    )
)