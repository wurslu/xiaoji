package com.example.thebest.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationPermissionManager(private val activity: ComponentActivity) {

    // 权限请求结果回调
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // 注册权限请求启动器
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 以下版本默认有权限，但还需要检查通知是否被用户关闭
            NotificationManagerCompat.from(activity).areNotificationsEnabled()
        }
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission()) {
                callback(true)
                return
            }

            onPermissionResult = callback
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Android 13 以下，检查通知是否启用
            callback(NotificationManagerCompat.from(activity).areNotificationsEnabled())
        }
    }

    /**
     * 检查并请求权限（如果需要）
     */
    fun checkAndRequestPermission(callback: (Boolean) -> Unit) {
        if (hasNotificationPermission()) {
            callback(true)
        } else {
            requestNotificationPermission(callback)
        }
    }

    companion object {
        /**
         * 检查通知权限（静态方法）- 修复版本
         */
        fun hasPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 13 以下也要检查用户是否禁用了通知
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }
}