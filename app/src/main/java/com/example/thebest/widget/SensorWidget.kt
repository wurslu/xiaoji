package com.example.thebest.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.thebest.MainActivity
import com.example.thebest.R
import com.example.thebest.data.network.NetworkModule
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class SensorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 为每个widget更新数据
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 第一个widget被添加时的处理
        startPeriodicUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个widget被移除时的处理
        stopPeriodicUpdate(context)
    }

    companion object {
        private const val WIDGET_UPDATE_ACTION = "com.example.thebest.WIDGET_UPDATE"
        private const val PREFS_NAME = "SensorWidgetPrefs"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_HUMIDITY = "humidity"
        private const val KEY_LIGHT = "light"
        private const val KEY_SOIL = "soil"
        private const val KEY_LAST_UPDATE = "last_update"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 获取缓存的传感器数据
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val temperature = prefs.getFloat(KEY_TEMPERATURE, 0f)
            val humidity = prefs.getFloat(KEY_HUMIDITY, 0f)
            val light = prefs.getInt(KEY_LIGHT, 0)
            val soil = prefs.getInt(KEY_SOIL, 0)
            val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)

            // 创建RemoteViews
            val views = RemoteViews(context.packageName, R.layout.sensor_widget_layout)

            // 更新数据显示
            views.setTextViewText(R.id.widget_temperature, "${temperature}°C")
            views.setTextViewText(R.id.widget_humidity, "${humidity}%")
            views.setTextViewText(R.id.widget_light, "$light")
            views.setTextViewText(R.id.widget_soil, if (soil == 1) "湿润" else "干燥")

            // 更新时间显示
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val updateTime = if (lastUpdate > 0) {
                timeFormat.format(Date(lastUpdate))
            } else {
                "--:--"
            }
            views.setTextViewText(R.id.widget_update_time, "更新: $updateTime")

            // 设置点击事件 - 打开主应用
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // 设置刷新按钮点击事件
            val refreshIntent = Intent(context, SensorWidgetProvider::class.java).apply {
                action = WIDGET_UPDATE_ACTION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // 更新widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateWidgetData(
            context: Context,
            temperature: Double,
            humidity: Double,
            light: Int,
            soil: Int
        ) {
            // 保存数据到SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat(KEY_TEMPERATURE, temperature.toFloat())
                putFloat(KEY_HUMIDITY, humidity.toFloat())
                putInt(KEY_LIGHT, light)
                putInt(KEY_SOIL, soil)
                putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                apply()
            }

            // 更新所有widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, SensorWidgetProvider::class.java)
            )

            for (widgetId in widgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun startPeriodicUpdate(context: Context) {
            // 使用AlarmManager定期更新（这里简化处理）
            // 实际项目中建议使用WorkManager
        }

        private fun stopPeriodicUpdate(context: Context) {
            // 停止定期更新
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == WIDGET_UPDATE_ACTION) {
            // 手动刷新请求
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId != -1) {
                // 异步获取最新数据
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 修复：添加 context 参数
                        val apiService = NetworkModule.provideApiService(
                            NetworkModule.provideHttpClient(),
                            context
                        )
                        val sensorData = apiService.getSensorData()

                        // 更新widget数据
                        withContext(Dispatchers.Main) {
                            updateWidgetData(
                                context,
                                sensorData.temperature,
                                sensorData.humidity,
                                sensorData.light,
                                sensorData.soil
                            )
                        }
                    } catch (e: Exception) {
                        // 网络请求失败时的处理
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
