package com.example.thebest.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.thebest.data.local.SensorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataExportUtils {

    companion object {
        private const val EXPORT_FOLDER = "sensor_exports"

        /**
         * 导出数据为CSV文件
         */
        suspend fun exportToCSV(
            context: Context,
            records: List<SensorRecord>,
            fileName: String = "sensor_data"
        ): Result<String> = withContext(Dispatchers.IO) {
            try {
                // 创建导出文件夹
                val exportDir = File(context.getExternalFilesDir(null), EXPORT_FOLDER)
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                // 生成文件名
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val csvFile = File(exportDir, "${fileName}_${timestamp}.csv")

                // 写入CSV内容
                FileWriter(csvFile).use { writer ->
                    // 写入标题行
                    writer.append("时间,温度(°C),湿度(%),光照强度,土壤湿度\n")

                    // 写入数据行
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    records.sortedBy { it.timestamp }.forEach { record ->
                        val timeStr = dateFormat.format(Date(record.timestamp))
                        writer.append("$timeStr,${record.temperature},${record.humidity},${record.light},${record.soil}\n")
                    }
                }

                Result.success(csvFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * 分享导出的文件
         */
        fun shareExportedFile(context: Context, filePath: String) {
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "传感器数据导出")
                    putExtra(Intent.EXTRA_TEXT, "这是从传感器监控应用导出的环境数据。")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "分享数据文件"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 获取导出统计信息
         */
        fun getExportSummary(records: List<SensorRecord>): ExportSummary {
            if (records.isEmpty()) {
                return ExportSummary(
                    totalRecords = 0,
                    dateRange = "无数据",
                    avgTemperature = 0.0,
                    avgHumidity = 0.0,
                    avgLight = 0.0
                )
            }

            val sortedRecords = records.sortedBy { it.timestamp }
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val startTime = dateFormat.format(Date(sortedRecords.first().timestamp))
            val endTime = dateFormat.format(Date(sortedRecords.last().timestamp))

            return ExportSummary(
                totalRecords = records.size,
                dateRange = "$startTime 至 $endTime",
                avgTemperature = records.map { it.temperature }.average(),
                avgHumidity = records.map { it.humidity }.average(),
                avgLight = records.map { it.light }.average()
            )
        }

        /**
         * 清理旧的导出文件（保留最近10个）
         */
        fun cleanOldExports(context: Context) {
            try {
                val exportDir = File(context.getExternalFilesDir(null), EXPORT_FOLDER)
                if (exportDir.exists()) {
                    val files = exportDir.listFiles()?.sortedByDescending { it.lastModified() }
                    files?.drop(10)?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    data class ExportSummary(
        val totalRecords: Int,
        val dateRange: String,
        val avgTemperature: Double,
        val avgHumidity: Double,
        val avgLight: Double
    )
}