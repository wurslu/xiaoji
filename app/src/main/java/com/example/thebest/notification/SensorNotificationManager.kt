package com.example.thebest.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.thebest.MainActivity
import com.example.thebest.R

class SensorNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "SENSOR_ALERTS"
        private const val CHANNEL_NAME = "环境监测提醒"
        private const val CHANNEL_DESCRIPTION = "传感器数据异常提醒"

        // 通知ID
        private const val NOTIFICATION_ID_TEMPERATURE = 1001
        private const val NOTIFICATION_ID_HUMIDITY = 1002
        private const val NOTIFICATION_ID_LIGHT = 1003
        private const val NOTIFICATION_ID_SOIL = 1004
    }

    init {
        createNotificationChannel()
    }

    // 创建通知渠道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    // 发送温度过高通知
    fun sendTemperatureAlert(temperature: Double, isHigh: Boolean) {
        // 只处理温度过高的情况
        if (isHigh) {
            val title = "温度过高警告"
            val message = "当前温度：${temperature}°C，请注意调节环境温度"

            sendNotification(
                id = NOTIFICATION_ID_TEMPERATURE,
                title = title,
                message = message,
                iconRes = R.drawable.ic_temperature_high
            )
        }
    }

    // 发送光照不足通知
    fun sendLightAlert(lightLevel: Int) {
        val title = "光照不足提醒"
        val message = "当前光照强度：$lightLevel，建议开启补光设备"

        sendNotification(
            id = NOTIFICATION_ID_LIGHT,
            title = title,
            message = message,
            iconRes = R.drawable.ic_light_low
        )
    }

    // 发送通用通知
    private fun sendNotification(
        id: Int,
        title: String,
        message: String,
        iconRes: Int = R.drawable.ic_notification_default
    ) {
        // 创建点击通知后的意图
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        // 发送通知
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // 处理通知权限被拒绝的情况
            e.printStackTrace()
        }
    }

    // 取消特定通知
    fun cancelNotification(notificationType: NotificationType) {
        val notificationId = when (notificationType) {
            NotificationType.TEMPERATURE -> NOTIFICATION_ID_TEMPERATURE
            NotificationType.HUMIDITY -> NOTIFICATION_ID_HUMIDITY
            NotificationType.LIGHT -> NOTIFICATION_ID_LIGHT
            NotificationType.SOIL -> NOTIFICATION_ID_SOIL
        }

        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    // 取消所有通知
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    // 检查通知权限
    fun hasNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

// 通知类型枚举
enum class NotificationType {
    TEMPERATURE,
    HUMIDITY,
    LIGHT,
    SOIL
}