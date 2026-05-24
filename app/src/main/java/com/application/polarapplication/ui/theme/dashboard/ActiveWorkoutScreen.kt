package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfo
import com.application.polarapplication.ui.info.MetricInfoData
import com.application.polarapplication.ui.stressPredictor.StressBodyVisualizer
import kotlinx.coroutines.launch

private val BgDark      = Color(0xFF080808)
private val GlassBg     = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)
private val CardBg      = Color(0xFF0F0F14)
private val CardBorder  = Color(0x1AFFFFFF)

private fun getZoneConfig(heartRate: Int, maxHr: Int): HrZoneConfig {
    val pct = heartRate.toFloat() / maxHr.toFloat()
    return when {
        pct >= 0.90f -> HrZoneConfig("Z5 · MAXIMUM",  "Z5", Color(0xFFEF4444), Color(0x1AEF4444), "HR at maximum · Z5",    "MAX",     Color(0xFFEF4444), "> 6 mmol/L")
        pct >= 0.80f -> HrZoneConfig("Z4 · ANAEROBIC","Z4", Color(0xFFF97316), Color(0x1AF97316), "Anaerobic effort · Z4", "HIGH",    Color(0xFFF97316), "4–6 mmol/L")
        pct >= 0.70f -> HrZoneConfig("Z3 · AEROBIC",  "Z3", Color(0xFF4ADE80), Color(0x1A4ADE80), "Aerobic effort · Z3",   "AEROBIC", Color(0xFF4ADE80), "2–4 mmol/L")
        pct >= 0.60f -> HrZoneConfig("Z2 · CONTROL",  "Z2", Color(0xFF60A5FA), Color(0x1A60A5FA), "HR stable · Z2 aerobic","CALM",    Color(0xFF60A5FA), "< 2 mmol/L")
        else         -> HrZoneConfig("Z1 · RECOVERY", "Z1", Color(0xFFA3E635), Color(0x1AA3E635), "Recovery zone · Z1",    "EASY",    Color(0xFF4ADE80), "< 1 mmol/L")
    }
}

private data class HrZoneConfig(
    val label: String, val shortLabel: String, val color: Color,
    val bgColor: Color, val aiStatus: String, val aiShort: String,
    val aiColor: Color, val lactate: String
)

@Composable
fun ActiveWorkoutScreen(
    viewModel:       DashboardViewModel,
    userGender:      String,
    onMinimizeClick: () -> Unit
) {
    val vitals          by viewModel.athleteVitals.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val workoutType     by viewModel.selectedWorkoutType.collectAsState()
    val maxHr           by viewModel.userMaxHr.collectAsState()
    val elapsedSeconds  by viewModel.elapsedSeconds.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }
    val timerStr = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    // ── Sample buffer ─────────────────────────────────────────────────────────
    val startTime = remember { System.currentTimeMillis() }
    val hrSamples = remember { mutableStateListOf<HrSample>() }

    LaunchedEffect(vitals.heartRate) {
        if (vitals.heartRate > 0) {
            hrSamples.add(HrSample(
                timestamp = System.currentTimeMillis(),
                heartRate = vitals.heartRate,
                zone      = calcZone(vitals.heartRate, maxHr)
            ))
            if (hrSamples.size > 500) hrSamples.removeAt(0)
        }
    }

    // ── Peak detection (background) ───────────────────────────────────────────
    val peaks = remember { mutableStateListOf<Peak>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(hrSamples.size) {
        if (hrSamples.size % 10 == 0 && hrSamples.size >= 20) {
            scope.launch {
                val detected = detectPeaks(hrSamples.toList())
                peaks.clear()
                peaks.addAll(detected)
            }
        }
    }

    // ── Sparkline histories ───────────────────────────────────────────────────
    val rmssdHistory = remember { mutableStateListOf<Float>() }
    val cnsHistory   = remember { mutableStateListOf<Float>() }
    val trimpHistory = remember { mutableStateListOf<Float>() }
    val calHistory   = remember { mutableStateListOf<Float>() }

    LaunchedEffect(vitals.rmssd, vitals.cnsScore, vitals.trimpScore, vitals.calories) {
        if (vitals.rmssd > 0)    { rmssdHistory.add(vitals.rmssd.toFloat());     if (rmssdHistory.size > 30) rmssdHistory.removeAt(0) }
        if (vitals.cnsScore > 0) { cnsHistory.add(vitals.cnsScore.toFloat());    if (cnsHistory.size > 30)   cnsHistory.removeAt(0) }
        trimpHistory.add(vitals.trimpScore.toFloat()); if (trimpHistory.size > 30) trimpHistory.removeAt(0)
        calHistory.add(vitals.calories.toFloat());     if (calHistory.size > 30)   calHistory.removeAt(0)
    }

    // ── Zone & animations ─────────────────────────────────────────────────────
    val zoneConfig     = remember(vitals.heartRate, maxHr) { getZoneConfig(vitals.heartRate, maxHr) }
    val hrFraction     = (vitals.heartRate.toFloat() / maxHr.toFloat()).coerceIn(0f, 1f)
    val stressFraction = ((hrFraction - 0.4f) / 0.5f).coerceIn(0f, 1f)

    val animatedZoneColor by animateColorAsState(zoneConfig.color, tween(500), label = "zone")
    val animatedAiColor   by animateColorAsState(zoneConfig.aiColor, tween(500), label = "ai")

    val targetTrimp = when (workoutType.uppercase()) {
        "STRENGTH" -> 60f; "ENDURANCE" -> 80f; "SPEED" -> 50f; "RECOVERY" -> 20f; else -> 0f
    }
    val trimpProgress = if (targetTrimp > 0) (vitals.trimpScore / targetTrimp).toFloat().coerceIn(0f, 1f) else 0f

    if (!isWorkoutActive) { LaunchedEffect(Unit) { onMinimizeClick() } }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TopBar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().background(GlassBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(workoutType.uppercase(), color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text(timerStr, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                }
                IconButton(onClick = onMinimizeClick) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f))
                }
            }

            // ── HR Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (vitals.heartRate > 0) "${vitals.heartRate}" else "—",
                        color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, lineHeight = 50.sp
                    )
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(animatedZoneColor))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("BPM", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(animatedZoneColor.copy(alpha = 0.12f))
                            .border(1.dp, animatedZoneColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(zoneConfig.shortLabel, color = animatedZoneColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(zoneConfig.lactate, color = animatedZoneColor.copy(alpha = 0.5f), fontSize = 10.sp)
                    Text("${(hrFraction * 100).toInt()}% max", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }

            // ── Hero: Omuleț + carduri ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp)).background(GlassBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    StressBodyVisualizer(
                        stressScore = stressFraction,
                        userGender  = userGender,
                        auraColor   = animatedZoneColor,
                        heartRate   = vitals.heartRate
                    )
                }

                Column(
                    modifier            = Modifier.width(110.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveMetricCard(
                        icon     = Icons.Default.Favorite,
                        iconTint = Color(0xFF60A5FA),
                        label    = "RMSSD",
                        value    = if (vitals.rmssd > 0) "${"%.0f".format(vitals.rmssd)}" else "—",
                        unit     = "ms",
                        sub      = "HRV live",
                        color    = Color(0xFF60A5FA),
                        history  = rmssdHistory,
                        barFrac  = (vitals.rmssd / 100.0).toFloat().coerceIn(0f, 1f),
                        info     = MetricInfoData.CNS,
                        modifier = Modifier.weight(1f)
                    )
                    LiveMetricCard(
                        icon     = Icons.Default.Psychology,
                        iconTint = Color(0xFF4ADE80),
                        label    = "CNS",
                        value    = if (vitals.cnsScore > 0) "${vitals.cnsScore}" else "—",
                        unit     = "%",
                        sub      = "Readiness",
                        color    = Color(0xFF4ADE80),
                        history  = cnsHistory,
                        barFrac  = vitals.cnsScore / 100f,
                        info     = MetricInfoData.CNS,
                        modifier = Modifier.weight(1f)
                    )
                    LiveMetricCard(
                        icon     = Icons.Default.FitnessCenter,
                        iconTint = Color(0xFFFBBF24),
                        label    = "TRIMP",
                        value    = "%.1f".format(vitals.trimpScore),
                        unit     = "",
                        sub      = "Load",
                        color    = Color(0xFFFBBF24),
                        history  = trimpHistory,
                        barFrac  = trimpProgress,
                        info     = MetricInfoData.TRIMP,
                        modifier = Modifier.weight(1f)
                    )
                    LiveMetricCard(
                        icon     = Icons.Default.Whatshot,
                        iconTint = Color(0xFFF97316),
                        label    = "CALORIES",
                        value    = "${vitals.calories}",
                        unit     = "kcal",
                        sub      = "Burned",
                        color    = Color(0xFFF97316),
                        history  = calHistory,
                        barFrac  = (vitals.calories / 500f).coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Chart Pager ───────────────────────────────────────────────────
            WorkoutChartPager(
                samples     = hrSamples.toList(),
                peaks       = peaks.toList(),
                maxHr       = maxHr,
                workoutType = workoutType,
                modifier    = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── AI Strip ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(11.dp)).background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(animatedAiColor))
                    Column {
                        Text("AI BIOMETRIC ANALYSIS", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Text(zoneConfig.aiStatus, color = animatedAiColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(5.dp))
                        .background(animatedAiColor.copy(alpha = 0.08f))
                        .border(1.dp, animatedAiColor.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(zoneConfig.aiShort, color = animatedAiColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Stop ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    .height(50.dp).clip(RoundedCornerShape(13.dp))
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

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor   = Color(0xFF1E1E24),
            title            = { Text("End session", color = Color.White) },
            text             = { Text("Save this workout session?", color = Color.Gray) },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.stopWorkout(workoutType)
                    showStopDialog = false
                    onMinimizeClick()
                }) { Text("SAVE & STOP", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
            },
            dismissButton    = {
                TextButton(onClick = { showStopDialog = false }) { Text("CONTINUE", color = Color.White) }
            }
        )
    }
}

// ─────────────────────────────────────────────
// LIVE METRIC CARD
// ─────────────────────────────────────────────

@Composable
private fun LiveMetricCard(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label:    String,
    value:    String,
    unit:     String,
    sub:      String,
    color:    Color,
    history:  List<Float>,
    barFrac:  Float,
    info:     com.application.polarapplication.ui.info.MetricInfo? = null,
    modifier: Modifier = Modifier
) {
    val animBar by animateFloatAsState(targetValue = barFrac, animationSpec = tween(600), label = "bar")

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(CardBg).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(10.dp))
                Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                if (info != null) InfoIconButton(info = info, tint = Color.White.copy(alpha = 0.15f))
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, lineHeight = 20.sp)
                if (unit.isNotEmpty()) {
                    Text(unit, color = color.copy(alpha = 0.7f), fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            if (history.size >= 3) {
                MiniLineChart(data = history, color = color, modifier = Modifier.fillMaxWidth().height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(sub, color = Color.White.copy(alpha = 0.18f), fontSize = 7.sp)
        }

        // Bară laterală
        Box(
            modifier = Modifier.width(3.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(animBar)
                    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.3f))))
            )
        }
    }
}

// ─────────────────────────────────────────────
// MINI LINE CHART
// ─────────────────────────────────────────────

@Composable
private fun MiniLineChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    Canvas(modifier = modifier) {
        val minV  = data.min()
        val maxV  = data.max().coerceAtLeast(minV + 1f)
        val range = maxV - minV
        val stepX = size.width / (data.size - 1)

        val fillPath = Path()
        fillPath.moveTo(0f, size.height)
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * size.height
            if (i == 0) fillPath.lineTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((data.size - 1) * stepX, size.height)
        fillPath.close()
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))

        val linePath = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * size.height
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, color.copy(alpha = 0.9f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val lastX = (data.size - 1) * stepX
        val lastY = size.height - ((data.last() - minV) / range) * size.height
        drawCircle(color, 2.dp.toPx(), Offset(lastX, lastY))
    }
}