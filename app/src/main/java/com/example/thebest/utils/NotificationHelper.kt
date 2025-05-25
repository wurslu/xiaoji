package com.example.thebest.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

object NotificationHelper {

    /**
     * 打开应用的通知设置页面
     * 这是你之前询问的 ACTION_APP_NOTIFICATION_SETTINGS 的用法
     */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } catch (e1: Exception) {
            // 如果无法打开应用专用设置，尝试打开通用通知设置
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                // 最后尝试打开应用信息页面
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e3: Exception) {
                    // 所有方法都失败，忽略
                }
            }
        }
    }

    /**
     * 打开通知监听器设置页面
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 打开通用设置
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }

    /**
     * 打开所有应用的通知设置页面
     */
    fun openAllAppsNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 降级到应用通知设置
            openAppNotificationSettings(context)
        }
    }
}