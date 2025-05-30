package com.example.thebest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.thebest.data.local.SensorDatabase
import com.example.thebest.data.network.NetworkModule
import com.example.thebest.data.repository.SensorRepository
import com.example.thebest.ui.compose.*
import com.example.thebest.ui.theme.TheBestTheme
import com.example.thebest.ui.viewmodel.DataManagementViewModel
import com.example.thebest.ui.viewmodel.HistoryViewModel
import com.example.thebest.ui.viewmodel.MainViewModel
import com.example.thebest.ui.viewmodel.SettingsViewModel
import com.example.thebest.utils.AutoCleanupManager
import com.example.thebest.utils.NotificationPermissionManager

class MainActivity : ComponentActivity() {

    private lateinit var notificationPermissionManager: NotificationPermissionManager
    private lateinit var autoCleanupManager: AutoCleanupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化通知权限管理器
        notificationPermissionManager = NotificationPermissionManager(this)

        // 网络模块和数据库
        val httpClient = NetworkModule.provideHttpClient()
        val apiService = NetworkModule.provideApiService(httpClient, this)
        val database = SensorDatabase.getDatabase(this)
        val sensorDao = database.sensorDao()
        val repository = SensorRepository(apiService, sensorDao)

        // 初始化自动清理管理器
        autoCleanupManager = AutoCleanupManager(this, repository)

        setContent {
            TheBestTheme {
                MainApp(repository, apiService, this@MainActivity)
            }
        }

        // 应用启动后的初始化操作
        initializeApp()
    }

    private fun initializeApp() {
        // 检查并请求通知权限
        checkNotificationPermission()

        // 启动自动清理服务
        autoCleanupManager.startAutoCleanup()

        // 新增：同步本地阈值设置到服务端
        syncThresholdsToServer()
    }


    private fun checkNotificationPermission() {
        notificationPermissionManager.checkAndRequestPermission { isGranted ->
            if (!isGranted) {
                // 只在权限被拒绝时显示提示
                Toast.makeText(this, "通知权限被拒绝，可能无法接收环境异常提醒", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    /**
     * 同步本地阈值设置到服务端
     */
    private fun syncThresholdsToServer() {
        // 需要获取 apiService 实例
        val httpClient = NetworkModule.provideHttpClient()
        val apiService = NetworkModule.provideApiService(httpClient, this)

        // 创建 SettingsViewModel 实例并调用同步方法
        val settingsViewModel = SettingsViewModel(apiService, this)
        settingsViewModel.syncThresholdsToServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        autoCleanupManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    repository: SensorRepository,
    apiService: com.example.thebest.data.network.ApiService,
    context: MainActivity
) {
    val navController = rememberNavController()

    // 导航项目
    val items = listOf(
        NavigationItem(
            title = "监控",
            icon = Icons.Default.Home,
            route = "sensor"
        ),
        NavigationItem(
            title = "历史",
            icon = Icons.Default.History,
            route = "history"
        ),
        NavigationItem(
            title = "设置",
            icon = Icons.Default.Settings,
            route = "settings"
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "sensor",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("sensor") {
                val viewModel = viewModel<MainViewModel> {
                    MainViewModel(repository, context)
                }
                SensorScreen(viewModel = viewModel)
            }

            composable("history") {
                val viewModel = viewModel<HistoryViewModel> { HistoryViewModel(repository) }
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { range ->
                        navController.navigate("history_detail/${range.name}")
                    }
                )
            }

            composable("history_detail/{rangeType}") { backStackEntry ->
                val rangeType = backStackEntry.arguments?.getString("rangeType")
                val range = HistoryViewModel.DateRange.valueOf(rangeType ?: "TODAY")
                val viewModel = viewModel<HistoryViewModel> { HistoryViewModel(repository) }
                HistoryDetailScreen(
                    viewModel = viewModel,
                    selectedRange = range,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                val viewModel = viewModel<SettingsViewModel> {
                    SettingsViewModel(apiService, context)
                }
                SettingsMainScreen(
                    viewModel = viewModel,
                    onNavigateToThresholds = {
                        navController.navigate("settings_thresholds")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("settings_notifications")
                    },
                    onNavigateToGeneral = {
                        navController.navigate("settings_general")
                    },
                    onNavigateToDataManagement = {
                        navController.navigate("settings_data_management")
                    },
                    onNavigateToAbout = {
                        navController.navigate("settings_about")
                    }
                )
            }

            composable("settings_thresholds") {
                val viewModel = viewModel<SettingsViewModel> {
                    SettingsViewModel(apiService, context)
                }
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings_notifications") {
                val viewModel = viewModel<SettingsViewModel> {
                    SettingsViewModel(apiService, context)
                }
                NotificationSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings_general") {
                val viewModel = viewModel<SettingsViewModel> {
                    SettingsViewModel(apiService, context)
                }
                GeneralSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings_data_management") {
                val viewModel = viewModel<DataManagementViewModel> {
                    DataManagementViewModel(repository, context)
                }
                DataManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings_about") {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)