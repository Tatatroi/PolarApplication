package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun WorkoutTimelineChart(
    samples:   List<HrSample>,
    peaks:     List<Peak>,
    maxHr:     Int,
    modifier:  Modifier = Modifier
) {
    if (samples.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Collecting data...", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }
        return
    }

    var tooltipIndex by remember { mutableStateOf<Int?>(null) }

    val minHr    = (samples.minOf { it.heartRate } - 10).coerceAtLeast(40)
    val maxHrVal = (samples.maxOf { it.heartRate } + 10).coerceAtMost(maxHr + 10)
    val yRange   = (maxHrVal - minHr).toFloat()
    val avgHr    = samples.map { it.heartRate }.average().toInt()

    val startTs  = samples.first().timestamp
    val endTs    = samples.last().timestamp
    val totalMs  = (endTs - startTs).coerceAtLeast(1L)

    Column(modifier = modifier) {
        // ── Canvas principale ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF0A0A0F))
                .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(samples) {
                        detectTapGestures { offset ->
                            val chartLeft  = 36.dp.toPx()
                            val chartRight = size.width - 8.dp.toPx()
                            val frac       = ((offset.x - chartLeft) / (chartRight - chartLeft)).coerceIn(0f, 1f)
                            val idx        = (frac * (samples.size - 1)).toInt()
                            tooltipIndex   = if (tooltipIndex == idx) null else idx
                        }
                    }
            ) {
                val chartLeft   = 36.dp.toPx()
                val chartRight  = size.width - 8.dp.toPx()
                val chartTop    = 10.dp.toPx()
                val chartBottom = size.height - 20.dp.toPx()
                val chartW      = chartRight - chartLeft
                val chartH      = chartBottom - chartTop

                fun hrToY(hr: Int)  = chartBottom - ((hr - minHr) / yRange) * chartH
                fun tsToX(ts: Long) = chartLeft + ((ts - startTs).toFloat() / totalMs) * chartW

                // ── Zone background bands ─────────────────────────────────────
                val zoneBands = listOf(
                    1 to (40 to (maxHr * 0.60).toInt()),
                    2 to ((maxHr * 0.60).toInt() to (maxHr * 0.70).toInt()),
                    3 to ((maxHr * 0.70).toInt() to (maxHr * 0.80).toInt()),
                    4 to ((maxHr * 0.80).toInt() to (maxHr * 0.90).toInt()),
                    5 to ((maxHr * 0.90).toInt() to maxHr)
                )
                zoneBands.forEach { (zone, range) ->
                    val yTop    = hrToY(range.second.coerceAtMost(maxHrVal)).coerceAtLeast(chartTop)
                    val yBot    = hrToY(range.first.coerceAtLeast(minHr)).coerceAtMost(chartBottom)
                    if (yTop < yBot) {
                        drawRect(
                            color    = zoneColor(zone).copy(alpha = 0.04f),
                            topLeft  = Offset(chartLeft, yTop),
                            size     = Size(chartW, yBot - yTop)
                        )
                    }
                }

                // ── Y axis labels ─────────────────────────────────────────────
                listOf(
                    (maxHr * 0.90).toInt(),
                    (maxHr * 0.80).toInt(),
                    (maxHr * 0.70).toInt(),
                    (maxHr * 0.60).toInt()
                ).forEach { bpm ->
                    if (bpm in minHr..maxHrVal) {
                        val y = hrToY(bpm)
                        drawLine(
                            color       = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
                            start       = Offset(chartLeft, y),
                            end         = Offset(chartRight, y),
                            strokeWidth = 1f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$bpm",
                            chartLeft - 4.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.argb(100, 255, 255, 255)
                                textSize  = 8.dp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // ── Avg HR linie punctată ─────────────────────────────────────
                val avgY     = hrToY(avgHr)
                val dashLen  = 6.dp.toPx()
                val gapLen   = 4.dp.toPx()
                var xCursor  = chartLeft
                while (xCursor < chartRight) {
                    drawLine(
                        color       = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                        start       = Offset(xCursor, avgY),
                        end         = Offset((xCursor + dashLen).coerceAtMost(chartRight), avgY),
                        strokeWidth = 1f
                    )
                    xCursor += dashLen + gapLen
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Avg: $avgHr",
                    chartRight,
                    avgY - 3.dp.toPx(),
                    android.graphics.Paint().apply {
                        color     = android.graphics.Color.argb(160, 255, 255, 255)
                        textSize  = 8.dp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )

                // ── HR line segmentata pe zone ────────────────────────────────
                for (i in 1 until samples.size) {
                    val prev    = samples[i - 1]
                    val curr    = samples[i]
                    val x1      = tsToX(prev.timestamp)
                    val y1      = hrToY(prev.heartRate)
                    val x2      = tsToX(curr.timestamp)
                    val y2      = hrToY(curr.heartRate)
                    val color   = zoneColor(curr.zone)

                    // Fill sub linie
                    val fillPath = Path().apply {
                        moveTo(x1, chartBottom)
                        lineTo(x1, y1)
                        lineTo(x2, y2)
                        lineTo(x2, chartBottom)
                        close()
                    }
                    drawPath(fillPath, color.copy(alpha = 0.08f))

                    drawLine(
                        color       = color,
                        start       = Offset(x1, y1),
                        end         = Offset(x2, y2),
                        strokeWidth = 2.5.dp.toPx(),
                        cap         = StrokeCap.Round
                    )
                }

                // ── Peak markers ──────────────────────────────────────────────
                peaks.forEach { peak ->
                    val x = tsToX(peak.timestamp)
                    val y = hrToY(peak.bpm)

                    // Triunghi în sus
                    val triSize = 5.dp.toPx()
                    val triPath = Path().apply {
                        moveTo(x, y - triSize * 1.5f)
                        lineTo(x - triSize, y)
                        lineTo(x + triSize, y)
                        close()
                    }
                    drawPath(triPath, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))

                    // Valoare deasupra
                    drawContext.canvas.nativeCanvas.drawText(
                        "${peak.bpm}",
                        x,
                        y - triSize * 2f,
                        android.graphics.Paint().apply {
                            color     = android.graphics.Color.WHITE
                            textSize  = 8.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }

                // ── Recovery indicators tra peak-uri ─────────────────────────
                for (i in 1 until peaks.size) {
                    val p1     = peaks[i - 1]
                    val p2     = peaks[i]
                    val x1     = tsToX(p1.timestamp)
                    val x2     = tsToX(p2.timestamp)
                    val midX   = (x1 + x2) / 2f
                    val recSec = (p2.timestamp - p1.timestamp) / 1000

                    var xC = x1
                    val dotGap = 4.dp.toPx()
                    while (xC < x2) {
                        drawCircle(
                            color  = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                            radius = 1.dp.toPx(),
                            center = Offset(xC, hrToY(minHr + (yRange * 0.15f).toInt()))
                        )
                        xC += dotGap
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${recSec}s rec",
                        midX,
                        hrToY(minHr + (yRange * 0.1f).toInt()),
                        android.graphics.Paint().apply {
                            color     = android.graphics.Color.argb(120, 255, 255, 255)
                            textSize  = 7.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // ── Tooltip ───────────────────────────────────────────────────
                tooltipIndex?.let { idx ->
                    if (idx < samples.size) {
                        val s   = samples[idx]
                        val x   = tsToX(s.timestamp)
                        val y   = hrToY(s.heartRate)
                        val sec = (s.timestamp - startTs) / 1000
                        val lbl = "${s.heartRate} BPM @ ${formatElapsed(sec)}"

                        drawLine(
                            color       = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                            start       = Offset(x, chartTop),
                            end         = Offset(x, chartBottom),
                            strokeWidth = 1f
                        )
                        drawCircle(zoneColor(s.zone), 4.dp.toPx(), Offset(x, y))
                        drawCircle(androidx.compose.ui.graphics.Color.White, 2.dp.toPx(), Offset(x, y))

                        val paint = android.graphics.Paint().apply {
                            color     = android.graphics.Color.WHITE
                            textSize  = 9.dp.toPx()
                            isFakeBoldText = true
                        }
                        val tw  = paint.measureText(lbl)
                        val bx  = (x - tw / 2f - 6.dp.toPx()).coerceIn(chartLeft, chartRight - tw - 12.dp.toPx())
                        val by  = (y - 28.dp.toPx()).coerceAtLeast(chartTop + 4.dp.toPx())

                        drawRoundRect(
                            color        = androidx.compose.ui.graphics.Color(0xF0111116),
                            topLeft      = Offset(bx, by),
                            size         = Size(tw + 12.dp.toPx(), 16.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawContext.canvas.nativeCanvas.drawText(lbl, bx + 6.dp.toPx(), by + 11.dp.toPx(), paint)
                    }
                }

                // ── X axis labels (tempo) ─────────────────────────────────────
                val totalSec = totalMs / 1000
                val step     = when {
                    totalSec < 120  -> 30L
                    totalSec < 300  -> 60L
                    totalSec < 600  -> 120L
                    else            -> 300L
                }
                var t = step
                while (t < totalSec) {
                    val x = chartLeft + (t.toFloat() / totalSec) * chartW
                    drawContext.canvas.nativeCanvas.drawText(
                        formatElapsed(t),
                        x,
                        size.height - 2.dp.toPx(),
                        android.graphics.Paint().apply {
                            color     = android.graphics.Color.argb(80, 255, 255, 255)
                            textSize  = 7.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                    t += step
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Zone time badges ──────────────────────────────────────────────────
        val distribution = remember(samples.size) { calculateZoneDistribution(samples) }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            distribution.filter { it.durationMs > 0 }.forEach { zd ->
                val sec   = zd.durationMs / 1000
                val color = zoneColor(zd.zone)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.12f))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${zoneName(zd.zone)}: ${formatElapsed(sec)}",
                        color      = color,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}