package com.example.thebest

import android.os.Bundle
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
import com.example.thebest.ui.compose.HistoryDetailScreen
import com.example.thebest.ui.compose.HistoryScreen
import com.example.thebest.ui.compose.SensorScreen
import com.example.thebest.ui.compose.SettingsScreen
import com.example.thebest.ui.theme.TheBestTheme
import com.example.thebest.ui.viewmodel.HistoryViewModel
import com.example.thebest.ui.viewmodel.MainViewModel
import com.example.thebest.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 网络模块
        val httpClient = NetworkModule.provideHttpClient()
        val apiService = NetworkModule.provideApiService(httpClient)

        val database = SensorDatabase.getDatabase(this)
        val sensorDao = database.sensorDao()

        val repository = SensorRepository(apiService, sensorDao)

        setContent {
            TheBestTheme {
                MainApp(repository, apiService, this@MainActivity)
            }
        }
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

    // 导航项目 - 新增历史数据页面
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
                    MainViewModel(repository, context) // 传递Context
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
                    SettingsViewModel(apiService)
                }
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)