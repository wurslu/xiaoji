package com.example.thebest.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
            // Android 13 以下版本不需要运行时权限
            true
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
            callback(true)
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
         * 检查通知权限（静态方法）
         */
        fun hasPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }
}