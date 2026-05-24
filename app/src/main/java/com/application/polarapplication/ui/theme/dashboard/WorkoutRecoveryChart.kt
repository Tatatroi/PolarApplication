package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkoutRecoveryChart(
    samples:  List<HrSample>,
    peaks:    List<Peak>,
    modifier: Modifier = Modifier
) {
    val recoveryEvents = remember(peaks.size, samples.size) {
        calculateRecoveryEvents(samples, peaks)
    }

    // Alertă oboseală: ultimele 2 recovery POOR
    val fatigue = recoveryEvents.size >= 2 &&
            recoveryEvents.takeLast(2).all { it.rating == RecoveryRating.POOR }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Recovery Analysis",
            color      = Color.White.copy(alpha = 0.6f),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Alertă oboseală ───────────────────────────────────────────────────
        if (fatigue) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFBBF24).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("⚠", fontSize = 14.sp)
                Text(
                    "Consider rest or lower intensity",
                    color      = Color(0xFFFBBF24),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (recoveryEvents.isEmpty()) {
            Box(
                modifier            = Modifier.fillMaxWidth().weight(1f),
                contentAlignment    = Alignment.Center
            ) {
                Text(
                    "No peak-recovery data yet.\nKeep training!",
                    color    = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
            }
            return@Column
        }

        // ── Trend sparkline ───────────────────────────────────────────────────
        if (recoveryEvents.size >= 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(10.dp)
            ) {
                Text(
                    "Recovery Trend",
                    color    = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val drops = recoveryEvents.map { it.hrDrop.toFloat() }
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    val minV  = drops.min()
                    val maxV  = drops.max().coerceAtLeast(minV + 1f)
                    val range = maxV - minV
                    val stepX = size.width / (drops.size - 1).coerceAtLeast(1)

                    val path = Path()
                    drops.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - ((v - minV) / range) * size.height
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    // Color del trend — verde se migliora, rosso se peggiora
                    val trendColor = if (drops.last() >= drops.first()) Color(0xFF4ADE80) else Color(0xFFF87171)

                    drawPath(path, trendColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drops.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - ((v - minV) / range) * size.height
                        drawCircle(trendColor, 3.dp.toPx(), Offset(x, y))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val improving = recoveryEvents.last().hrDrop >= recoveryEvents.first().hrDrop
                Text(
                    if (improving) "Recovery improving ↑" else "Recovery declining ↓",
                    color    = if (improving) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Lista eventi recovery ─────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recoveryEvents.indices.toList()) { idx ->
                val ev    = recoveryEvents[idx]
                val color = ev.rating.color
                val zoneC = zoneColor(ev.peak.zone)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0F14))
                        .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    // Bară colorată sânga
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(color)
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Header
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sprint #${idx + 1}",
                                color      = Color.White,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.12f))
                                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(ev.rating.label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Peak info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(zoneC.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(zoneName(ev.peak.zone), color = zoneC, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${ev.peak.bpm} BPM @ ${formatElapsed(ev.peak.elapsedSec)}",
                                color    = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Recovery info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("↓", color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(
                                "${formatElapsed(ev.recoveryTimeSec)} recovery → -${ev.hrDrop} BPM",
                                color    = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Recovery: -${ev.hrDrop} BPM ${if (ev.hrDrop >= 30) "✓" else if (ev.hrDrop >= 20) "~" else "✗"}",
                            color    = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}