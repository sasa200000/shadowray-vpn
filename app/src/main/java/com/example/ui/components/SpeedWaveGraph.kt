package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.util.Formatters
import com.example.util.LocalizationHelper

@Composable
fun SpeedWaveGraph(
    speedHistory: List<Pair<Long, Long>>, // (DownloadBytes, UploadBytes)
    currentDownSpeed: Long,
    currentUpSpeed: Long,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = LocalizationHelper.getString("speed_chart_title", language),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = LocalizationHelper.getString("speed_chart_desc", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Legend indicators
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DL: ${Formatters.formatSpeed(currentDownSpeed)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ElectricEmerald)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "UL: ${Formatters.formatSpeed(currentUpSpeed)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = ElectricEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Canvas Graph
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val width = size.width
            val height = size.height

            // Grid guidelines
            val gridColor = Color(0x1A94A3B8)
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, height * 0.25f), end = androidx.compose.ui.geometry.Offset(width, height * 0.25f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, height * 0.5f), end = androidx.compose.ui.geometry.Offset(width, height * 0.5f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, height * 0.75f), end = androidx.compose.ui.geometry.Offset(width, height * 0.75f), strokeWidth = 1.dp.toPx())

            if (speedHistory.isEmpty()) {
                // Flat line
                drawLine(
                    color = NeonCyan.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(0f, height - 4f),
                    end = androidx.compose.ui.geometry.Offset(width, height - 4f),
                    strokeWidth = 2.dp.toPx()
                )
                return@Canvas
            }

            val maxSpeed = (speedHistory.maxOfOrNull { maxOf(it.first, it.second) } ?: 1_000_000L)
                .coerceAtLeast(500_000L).toFloat()

            val stepX = width / (speedHistory.size - 1).coerceAtLeast(1)

            // Draw Download Curve (Cyan)
            val downPath = Path()
            val downFillPath = Path()
            downFillPath.moveTo(0f, height)

            speedHistory.forEachIndexed { index, pair ->
                val x = index * stepX
                val normalizedY = height - (pair.first.toFloat() / maxSpeed * (height * 0.85f))
                val y = normalizedY.coerceIn(5f, height - 2f)

                if (index == 0) {
                    downPath.moveTo(x, y)
                    downFillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevPair = speedHistory[index - 1]
                    val prevY = (height - (prevPair.first.toFloat() / maxSpeed * (height * 0.85f))).coerceIn(5f, height - 2f)
                    val cx = (prevX + x) / 2f
                    downPath.cubicTo(cx, prevY, cx, y, x, y)
                    downFillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            downFillPath.lineTo(width, height)
            downFillPath.close()

            // Fill area
            drawPath(
                path = downFillPath,
                brush = Brush.verticalGradient(
                    listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            // Stroke line
            drawPath(
                path = downPath,
                color = NeonCyan,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Upload Curve (Emerald)
            val upPath = Path()
            speedHistory.forEachIndexed { index, pair ->
                val x = index * stepX
                val normalizedY = height - (pair.second.toFloat() / maxSpeed * (height * 0.85f))
                val y = normalizedY.coerceIn(5f, height - 2f)

                if (index == 0) {
                    upPath.moveTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevPair = speedHistory[index - 1]
                    val prevY = (height - (prevPair.second.toFloat() / maxSpeed * (height * 0.85f))).coerceIn(5f, height - 2f)
                    val cx = (prevX + x) / 2f
                    upPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            drawPath(
                path = upPath,
                color = ElectricEmerald,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
