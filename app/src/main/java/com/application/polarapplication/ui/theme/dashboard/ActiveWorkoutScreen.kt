package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfo
import com.application.polarapplication.ui.info.MetricInfoData
import com.application.polarapplication.ui.stressPredictor.StressBodyVisualizer

// ─────────────────────────────────────────────
// ZONE CONFIG
// ─────────────────────────────────────────────

private data class HrZoneConfig(
    val label: String,
    val shortLabel: String,
    val color: Color,
    val bgColor: Color,
    val aiStatus: String,
    val aiShort: String,
    val aiColor: Color,
    val lactate: String
)

private fun getZoneConfig(heartRate: Int, maxHr: Int): HrZoneConfig {
    val pct = heartRate.toFloat() / maxHr.toFloat()
    return when {
        pct >= 0.90f -> HrZoneConfig("Z5 · MAXIMUM", "Z5", Color(0xFFEF4444), Color(0x1AEF4444), "HR at maximum · Z5", "MAX", Color(0xFFEF4444), "> 6 mmol/L")
        pct >= 0.80f -> HrZoneConfig("Z4 · ANAEROBIC", "Z4", Color(0xFFF97316), Color(0x1AF97316), "Anaerobic effort · Z4", "HIGH", Color(0xFFF97316), "4–6 mmol/L")
        pct >= 0.70f -> HrZoneConfig("Z3 · AEROBIC", "Z3", Color(0xFF4ADE80), Color(0x1A4ADE80), "Aerobic effort · Z3", "AEROBIC", Color(0xFF4ADE80), "2–4 mmol/L")
        pct >= 0.60f -> HrZoneConfig("Z2 · CONTROL", "Z2", Color(0xFF60A5FA), Color(0x1A60A5FA), "HR stable · Z2 aerobic", "CALM", Color(0xFF60A5FA), "< 2 mmol/L")
        else -> HrZoneConfig("Z1 · RECOVERY", "Z1", Color(0xFFA3E635), Color(0x1AA3E635), "Recovery zone · Z1", "EASY", Color(0xFF4ADE80), "< 1 mmol/L")
    }
}

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────

private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)
private val GlassSmBg = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@Composable
fun ActiveWorkoutScreen(
    viewModel: DashboardViewModel,
    userGender: String,
    onMinimizeClick: () -> Unit
) {
    val vitals by viewModel.athleteVitals.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val workoutType by viewModel.selectedWorkoutType.collectAsState()
    val maxHr by viewModel.userMaxHr.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }

    // ── Timer ─────────────────────────────────────────────────────────────────
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val timerStr = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    // ── HR History pentru trend/drift ─────────────────────────────────────────
    val hrHistory = remember { mutableStateListOf<Int>() }
    LaunchedEffect(vitals.heartRate) {
        if (vitals.heartRate > 0) {
            hrHistory.add(vitals.heartRate)
            if (hrHistory.size > 300) hrHistory.removeAt(0)
        }
    }

    val hrTrend: String = remember(hrHistory.size) {
        if (hrHistory.size < 10) {
            "—"
        } else {
            val last10 = hrHistory.takeLast(10).average()
            val prev10 = hrHistory.dropLast(10).takeLast(10).average()
            when {
                last10 > prev10 + 2 -> "↑ Rising"
                last10 < prev10 - 2 -> "↓ Falling"
                else -> "→ Stable"
            }
        }
    }
    val hrTrendColor = when {
        hrTrend.startsWith("↑") -> Color(0xFFF87171)
        hrTrend.startsWith("↓") -> Color(0xFF60A5FA)
        else -> Color(0xFF4ADE80)
    }

    val hrDrift: String = remember(hrHistory.size) {
        if (hrHistory.size < 150) {
            "—"
        } else {
            val now = hrHistory.takeLast(10).average()
            val before = hrHistory.dropLast(140).takeLast(10).average()
            val delta = (now - before).toInt()
            if (delta >= 0) "+$delta bpm" else "$delta bpm"
        }
    }

    // ── Zone & animations ─────────────────────────────────────────────────────
    val zoneConfig = remember(vitals.heartRate, maxHr) { getZoneConfig(vitals.heartRate, maxHr) }
    val hrFraction = (vitals.heartRate.toFloat() / maxHr.toFloat()).coerceIn(0f, 1f)
    val stressFraction = ((hrFraction - 0.4f) / 0.5f).coerceIn(0f, 1f)

    val animatedHrFraction by animateFloatAsState(targetValue = hrFraction, animationSpec = tween(600), label = "hr")
    val animatedZoneColor by animateColorAsState(targetValue = zoneConfig.color, animationSpec = tween(500), label = "zone")
    val animatedAiColor by animateColorAsState(targetValue = zoneConfig.aiColor, animationSpec = tween(500), label = "ai")

    // ── Plan target ───────────────────────────────────────────────────────────
    val targetTrimp = when (workoutType.uppercase()) {
        "STRENGTH" -> 60f; "ENDURANCE" -> 80f; "SPEED" -> 50f; "RECOVERY" -> 20f; else -> 0f
    }
    val trimpProgress = if (targetTrimp > 0) (vitals.trimpScore / targetTrimp).toFloat().coerceIn(0f, 1f) else 0f

    if (!isWorkoutActive) {
        LaunchedEffect(Unit) { onMinimizeClick() }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TopBar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(workoutType.uppercase(), color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text(timerStr, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                }
                IconButton(onClick = onMinimizeClick) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f))
                }
            }

            // ── Hero ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bara HR
                HrVerticalBar(
                    heartRate = vitals.heartRate,
                    hrFraction = animatedHrFraction,
                    zoneColor = animatedZoneColor,
                    zoneConfig = zoneConfig,
                    modifier = Modifier.width(52.dp).fillMaxHeight()
                )

                // Omuleț
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    StressBodyVisualizer(stressScore = stressFraction, userGender = userGender)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(workoutType, color = Color.White.copy(alpha = 0.3f), fontSize = 6.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                // Info dreapta
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallInfoCard("HR TREND", hrTrend, "last 60 sec", hrTrendColor, Modifier.weight(1f))
                    SmallInfoCard("HR DRIFT", hrDrift, "vs 5 min ago", Color.White, Modifier.weight(1f))
                    SmallInfoCard("RMSSD", if (vitals.rmssd > 0) "${"%.0f".format(vitals.rmssd)} ms" else "—", "HRV live", Color(0xFF60A5FA), Modifier.weight(1f))
                    SmallInfoCard("CNS", if (vitals.cnsScore > 0) "${vitals.cnsScore}%" else "—", "readiness", Color(0xFF4ADE80), Modifier.weight(1f))
                }
            }

            // ── Plan Progress ─────────────────────────────────────────────────
            if (targetTrimp > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 7.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF818CF8).copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.13f), RoundedCornerShape(11.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Target TRIMP: ${"%.0f".format(targetTrimp)} · Now: ${"%.1f".format(vitals.trimpScore)}",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.width(120.dp).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(trimpProgress).fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF818CF8))
                            )
                        }
                    }
                    Text("${(trimpProgress * 100).toInt()}%", color = Color(0xFF818CF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── AI Strip ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 7.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(animatedAiColor))
                    Column {
                        Text("AI BIOMETRIC ANALYSIS", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Text(zoneConfig.aiStatus, color = animatedAiColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(animatedAiColor.copy(alpha = 0.08f))
                        .border(1.dp, animatedAiColor.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(zoneConfig.aiShort, color = animatedAiColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }

            // ── Metrici ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricCard("TRIMP", "%.1f".format(vitals.trimpScore), "", trimpProgress, Color(0xFF818CF8), Modifier.weight(1f), true, MetricInfoData.TRIMP)
                MetricCard("CALORIES", "${vitals.calories}", "kcal", (vitals.calories / 500f).coerceIn(0f, 1f), Color.White.copy(alpha = 0.3f), Modifier.weight(1f))
            }

            // ── Stop ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.07f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { showStopDialog = true }, modifier = Modifier.fillMaxSize()) {
                    Text("END WORKOUT", color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = { Text("End session", color = Color.White) },
            text = { Text("Save this workout session?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopWorkout(workoutType)
                    showStopDialog = false
                    onMinimizeClick()
                }) { Text("SAVE & STOP", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("CONTINUE", color = Color.White) }
            }
        )
    }
}

// ─────────────────────────────────────────────
// HR VERTICAL BAR
// ─────────────────────────────────────────────

@Composable
private fun HrVerticalBar(
    heartRate: Int,
    hrFraction: Float,
    zoneColor: Color,
    zoneConfig: HrZoneConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(zoneColor.copy(alpha = 0.12f))
                .border(1.dp, zoneColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(zoneConfig.shortLabel, color = zoneColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        }

        Text(
            text = when (zoneConfig.shortLabel) {
                "Z5" -> "MAX"; "Z4" -> "ANA"; "Z3" -> "AER"; "Z2" -> "CTRL"; else -> "REC"
            },
            color = zoneColor.copy(alpha = 0.5f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(24.dp).weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val animFrac by animateFloatAsState(targetValue = hrFraction, animationSpec = tween(600), label = "bar")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animFrac)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(zoneColor, zoneColor.copy(alpha = 0.3f))))
            )
        }

        Text("$heartRate", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
        Text("BPM", color = Color.White.copy(alpha = 0.25f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(zoneConfig.lactate, color = zoneColor.copy(alpha = 0.4f), fontSize = 7.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────
// SMALL INFO CARD
// ─────────────────────────────────────────────

@Composable
private fun SmallInfoCard(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.18f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 15.sp)
        Text(sub, color = Color.White.copy(alpha = 0.18f), fontSize = 7.sp)
    }
}

// ─────────────────────────────────────────────
// METRIC CARD
// ─────────────────────────────────────────────

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    barFrac: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
    showInfo: Boolean = false,
    info: MetricInfo? = null
) {
    val animFrac by animateFloatAsState(targetValue = barFrac, animationSpec = tween(800), label = "bar")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (showInfo && info != null) {
                InfoIconButton(info = info, tint = Color.White.copy(alpha = 0.15f))
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
        if (unit.isNotEmpty()) {
            Text(unit, color = Color.White.copy(alpha = 0.18f), fontSize = 8.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFrac).fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}
