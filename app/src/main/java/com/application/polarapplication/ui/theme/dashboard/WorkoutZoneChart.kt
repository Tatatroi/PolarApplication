package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkoutZoneChart(
    samples:       List<HrSample>,
    targetZone:    Int = 3,   // zona target a antrenamentului
    modifier:      Modifier = Modifier
) {
    val distribution = remember(samples.size) { calculateZoneDistribution(samples) }

    if (distribution.isEmpty() || distribution.all { it.durationMs == 0L }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Collecting data...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }
        return
    }

    val totalSec = distribution.sumOf { it.durationMs } / 1000

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Zone Distribution",
            color      = Color.White.copy(alpha = 0.6f),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Bar chart ─────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Bottom
        ) {
            distribution.forEach { zd ->
                val color    = zoneColor(zd.zone)
                val sec      = zd.durationMs / 1000
                val isTarget = zd.zone == targetZone

                val animFrac by animateFloatAsState(
                    targetValue   = zd.percentage,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label         = "zone${zd.zone}"
                )

                Column(
                    modifier            = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Percentuale
                    Text(
                        "${(zd.percentage * 100).toInt()}%",
                        color      = if (zd.durationMs > 0) color else Color.White.copy(alpha = 0.2f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Bară
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animFrac.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (zd.durationMs > 0) color.copy(alpha = 0.8f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .then(
                                if (isTarget) Modifier.border(
                                    1.dp,
                                    color,
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                ) else Modifier
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Zone label
                    Text(
                        zoneName(zd.zone),
                        color      = if (zd.durationMs > 0) color else Color.White.copy(alpha = 0.2f),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Tempo
                    if (sec > 0) {
                        Text(
                            formatElapsed(sec),
                            color    = Color.White.copy(alpha = 0.35f),
                            fontSize = 8.sp
                        )
                    }

                    // Target badge
                    if (isTarget) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("TARGET", color = color, fontSize = 6.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // ── Summary ───────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Session Summary",
                color      = Color.White.copy(alpha = 0.4f),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            distribution.filter { it.durationMs > 0 }.forEach { zd ->
                val color    = zoneColor(zd.zone)
                val sec      = zd.durationMs / 1000
                val isTarget = zd.zone == targetZone
                val pct      = (zd.percentage * 100).toInt()

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Text(
                            "${zoneName(zd.zone)} · ${zoneFullName(zd.zone)}",
                            color    = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        if (isTarget) {
                            Text("★", color = color, fontSize = 9.sp)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(formatElapsed(sec), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("$pct%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}