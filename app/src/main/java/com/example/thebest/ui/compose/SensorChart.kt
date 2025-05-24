package com.example.thebest.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.thebest.data.local.SensorRecord
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.roundToInt

@Composable
fun SensorChartsSection(
    records: List<SensorRecord>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        // 空状态
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 数据概览卡片
        DataOverviewCard(records = records)

        // 温度图表
        SingleSensorChart(
            title = "温度趋势",
            records = records,
            icon = Icons.Default.DeviceThermostat,
            iconColor = MaterialTheme.colorScheme.error,
            dataExtractor = { it.temperature.toFloat() },
            unit = "°C",
            yAxisRange = 0f to 50f
        )

        // 湿度图表
        SingleSensorChart(
            title = "湿度趋势",
            records = records,
            icon = Icons.Default.WaterDrop,
            iconColor = MaterialTheme.colorScheme.primary,
            dataExtractor = { it.humidity.toFloat() },
            unit = "%",
            yAxisRange = 0f to 100f
        )

        // 光照图表
        SingleSensorChart(
            title = "光照趋势",
            records = records,
            icon = Icons.Default.WbSunny,
            iconColor = MaterialTheme.colorScheme.tertiary,
            dataExtractor = { it.light.toFloat() },
            unit = "Lux",
            yAxisRange = null // 自动范围
        )
    }
}

@Composable
fun DataOverviewCard(
    records: List<SensorRecord>
) {
    if (records.isEmpty()) {
        return
    }

    val latestRecord = records.lastOrNull()
    val avgTemp = records.map { it.temperature }.average()
    val avgHumidity = records.map { it.humidity }.average()
    val avgLight = records.map { it.light }.average()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "数据概览",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewItem(
                    icon = Icons.Default.DeviceThermostat,
                    label = "平均温度",
                    value = "${avgTemp.roundToInt()}°C",
                    current = "${latestRecord?.temperature?.roundToInt() ?: 0}°C",
                    color = MaterialTheme.colorScheme.error
                )

                OverviewItem(
                    icon = Icons.Default.WaterDrop,
                    label = "平均湿度",
                    value = "${avgHumidity.roundToInt()}%",
                    current = "${latestRecord?.humidity?.roundToInt() ?: 0}%",
                    color = MaterialTheme.colorScheme.primary
                )

                OverviewItem(
                    icon = Icons.Default.WbSunny,
                    label = "平均光照",
                    value = "${avgLight.roundToInt()}",
                    current = "${latestRecord?.light?.toInt() ?: 0}",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun OverviewItem(
    icon: ImageVector,
    label: String,
    value: String,
    current: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = "当前: $current",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SingleSensorChart(
    title: String,
    records: List<SensorRecord>,
    icon: ImageVector,
    iconColor: Color,
    dataExtractor: (SensorRecord) -> Float,
    unit: String,
    yAxisRange: Pair<Float, Float>?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 图表标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 单个传感器图表
            SingleLineChart(
                records = records,
                dataExtractor = dataExtractor,
                lineColor = iconColor,
                unit = unit,
                yAxisRange = yAxisRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
fun SingleLineChart(
    records: List<SensorRecord>,
    dataExtractor: (SensorRecord) -> Float,
    lineColor: Color,
    unit: String,
    yAxisRange: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                // 基本设置
                description.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setPinchZoom(true)

                // X轴设置
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.LTGRAY
                    textColor = android.graphics.Color.GRAY
                    setLabelCount(5, false)
                }

                // Y轴设置
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.LTGRAY
                    textColor = android.graphics.Color.GRAY
                    yAxisRange?.let { range ->
                        axisMinimum = range.first
                        axisMaximum = range.second
                    }
                }
                axisRight.isEnabled = false

                // 隐藏图例
                legend.isEnabled = false
            }
        },
        modifier = modifier,
        update = { chart ->
            // 按时间戳排序数据
            val sortedRecords = records.sortedBy { it.timestamp }

            val entries = sortedRecords.mapIndexed { index, record ->
                Entry(index.toFloat(), dataExtractor(record))
            }

            val dataSet = LineDataSet(entries, unit).apply {
                color = lineColor.toArgb()
                setCircleColor(lineColor.toArgb())
                lineWidth = 2.5f
                circleRadius = 3f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = lineColor.copy(alpha = 0.3f).toArgb()
            }

            chart.data = LineData(dataSet)
            chart.animateX(800)
            chart.invalidate()
        }
    )
}