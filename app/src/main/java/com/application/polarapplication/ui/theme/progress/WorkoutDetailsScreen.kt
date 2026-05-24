package com.application.polarapplication.ui.theme.progress

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfoData
import com.application.polarapplication.ui.theme.dashboard.HrSample
import com.application.polarapplication.ui.theme.dashboard.Peak
import com.application.polarapplication.ui.theme.dashboard.RecoveryEvent
import com.application.polarapplication.ui.theme.dashboard.RecoveryRating
import com.application.polarapplication.ui.theme.dashboard.calcZone
import com.application.polarapplication.ui.theme.dashboard.calculateRecoveryEvents
import com.application.polarapplication.ui.theme.dashboard.calculateZoneDistribution
import com.application.polarapplication.ui.theme.dashboard.detectPeaks
import com.application.polarapplication.ui.theme.dashboard.formatElapsed
import com.application.polarapplication.ui.theme.dashboard.zoneColor
import com.application.polarapplication.ui.theme.dashboard.zoneName
import com.application.polarapplication.ui.theme.dashboard.zoneFullName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

private val BgDark    = Color(0xFF080808)
private val CardDark  = Color(0xFF111116)
private val Zone5Color = Color(0xFFEF4444)
private val Zone4Color = Color(0xFFF97316)
private val Zone3Color = Color(0xFF4ADE80)
private val Zone2Color = Color(0xFF60A5FA)
private val Zone1Color = Color(0xFF9CA3AF)

private fun hrToZone(hr: Int, maxHr: Int): Int {
    val pct = hr.toFloat() / maxHr
    return when {
        pct >= 0.90f -> 5; pct >= 0.80f -> 4; pct >= 0.70f -> 3; pct >= 0.60f -> 2; else -> 1
    }
}

private fun hrToLactate(hr: Int, maxHr: Int) = when {
    hr.toFloat() / maxHr >= 0.90f -> "> 6 mmol/L"
    hr.toFloat() / maxHr >= 0.85f -> "4–6 mmol/L"
    hr.toFloat() / maxHr >= 0.70f -> "2–4 mmol/L"
    else -> "< 2 mmol/L"
}

private fun hrToLactateColor(hr: Int, maxHr: Int) = when {
    hr.toFloat() / maxHr >= 0.90f -> Color(0xFFEF4444)
    hr.toFloat() / maxHr >= 0.85f -> Color(0xFFF97316)
    hr.toFloat() / maxHr >= 0.70f -> Color(0xFFFBBF24)
    else -> Color(0xFF60A5FA)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutDetailsScreen(session: TrainingSessionEntity, maxHr: Int = 200) {

    val hrList: List<Int> = remember(session.hrSamples) {
        try {
            val type = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(session.hrSamples, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    val totalDurationSeconds = if (session.durationSeconds > 0) session.durationSeconds
    else hrList.size * 5L
    val secondsPerSample = if (hrList.isNotEmpty())
        (totalDurationSeconds.toFloat() / hrList.size).toLong().coerceAtLeast(1L)
    else 5L

    // Ricostruzione HrSample per algoritmi
    val hrSamples: List<HrSample> = remember(hrList, session.durationSeconds) {
        if (hrList.isEmpty()) return@remember emptyList()
        val totalMs    = totalDurationSeconds * 1000L
        val intervalMs = totalMs / hrList.size
        val startTs    = session.date - totalMs
        hrList.mapIndexed { i, hr ->
            HrSample(startTs + i * intervalMs, hr, calcZone(hr, maxHr))
        }
    }

    val peaks = remember { mutableStateListOf<Peak>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(hrSamples.size) {
        if (hrSamples.size >= 10) {
            scope.launch { val d = detectPeaks(hrSamples); peaks.clear(); peaks.addAll(d) }
        }
    }

    val durationStr = "%02d:%02d".format(totalDurationSeconds / 60, totalDurationSeconds % 60)
    val dateStr = remember(session.date) {
        java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(session.date))
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
            .padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Header ────────────────────────────────────────────────────────────
        Text(session.type.uppercase(), color = Color.White, fontSize = 28.sp,
            fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        ) {
            Text(dateStr, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Text(durationStr, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Text("${session.totalCalories} kcal", color = Color(0xFFFBBF24).copy(alpha = 0.8f),
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // ── Quick stats ───────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickStatCard("AVG HR",  "${session.avgHeartRate}", "bpm", Color(0xFFF87171),  Modifier.weight(1f))
            QuickStatCard("MAX HR",  "${session.maxHeartRate}", "bpm", Color(0xFFEF4444),  Modifier.weight(1f))
            QuickStatCard("TRIMP",   "%.1f".format(session.finalTrimp), "", Color(0xFFFBBF24), Modifier.weight(1f))
            QuickStatCard("CNS END", "${session.cnsScoreAtEnd}", "%",  Color(0xFF4ADE80),  Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Session Details ───────────────────────────────────────────────────
        if (session.activityType.isNotEmpty() || session.sessionGoal.isNotEmpty() ||
            session.focusArea.isNotEmpty() || session.rpe > 0) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(CardDark)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("SESSION DETAILS", color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp))
                if (session.activityType.isNotEmpty()) DetailRow("Activity",   session.activityType)
                if (session.sessionGoal.isNotEmpty())  DetailRow("Goal",       session.sessionGoal)
                if (session.focusArea.isNotEmpty())    DetailRow("Focus Area", session.focusArea)
                if (session.rpe > 0) {
                    val rpeLabel = when {
                        session.rpe <= 2 -> "Very Easy"; session.rpe <= 4 -> "Easy"
                        session.rpe <= 6 -> "Moderate";  session.rpe <= 8 -> "Hard"
                        else             -> "Maximum"
                    }
                    DetailRow("RPE", "${session.rpe}/10 · $rpeLabel")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Grafic HR original (păstrat) ──────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Heart rate zones", color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            InfoIconButton(info = MetricInfoData.HR_ZONES_OVERVIEW, tint = Color.White.copy(alpha = 0.25f))
        }

        HrChartCard(
            hrList = hrList, maxHr = maxHr,
            totalDurationSeconds = totalDurationSeconds, secondsPerSample = secondsPerSample
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Zone breakdown original (păstrat) ─────────────────────────────────
        if (hrList.isNotEmpty()) {
            ZoneBreakdownSection(hrList = hrList, maxHr = maxHr, secondsPerSample = secondsPerSample)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── ZONE DISTRIBUTION + RECOVERY — tab-uri noi ───────────────────────
        if (hrSamples.size >= 5) {
            val pagerState   = rememberPagerState(pageCount = { 2 })
            val tabLabels    = listOf("Zone Distribution", "Recovery Analysis")
            val recoveryEvents = remember(peaks.size, hrSamples.size) {
                calculateRecoveryEvents(hrSamples, peaks.toList())
            }

            // Tab selector
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabLabels.forEachIndexed { idx, label ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier.weight(1f).height(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isSelected) Color(0xFF818CF8).copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                if (isSelected) 1.dp else 0.dp,
                                if (isSelected) Color(0xFF818CF8).copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color      = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.35f),
                            fontSize   = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxWidth().height(
                    if (pagerState.currentPage == 0) 320.dp
                    else (100 + recoveryEvents.size * 110).dp.coerceAtLeast(200.dp)
                )
            ) { page ->
                when (page) {
                    0 -> ZoneDistributionView(
                        hrSamples   = hrSamples,
                        workoutType = session.type,
                        modifier    = Modifier.fillMaxSize()
                    )
                    1 -> RecoveryView(
                        recoveryEvents = recoveryEvents,
                        modifier       = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Athletic Impact ───────────────────────────────────────────────────
        AthleticImpactSection(session = session)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────
// ZONE DISTRIBUTION VIEW
// ─────────────────────────────────────────────

@Composable
private fun ZoneDistributionView(
    hrSamples:   List<HrSample>,
    workoutType: String,
    modifier:    Modifier = Modifier
) {
    val distribution = remember(hrSamples.size) { calculateZoneDistribution(hrSamples) }
    val targetZone   = when (workoutType.uppercase()) {
        "ENDURANCE" -> 3; "SPEED" -> 4; "STRENGTH" -> 3; "RECOVERY" -> 2; else -> 3
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Bar chart
        Row(
            modifier              = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Bottom
        ) {
            distribution.forEach { zd ->
                val color    = zoneColor(zd.zone)
                val sec      = zd.durationMs / 1000
                val isTarget = zd.zone == targetZone

                Column(
                    modifier            = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (zd.durationMs > 0) {
                        Text("${(zd.percentage * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .fillMaxHeight(zd.percentage.coerceAtLeast(if (zd.durationMs > 0) 0.03f else 0f))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(if (zd.durationMs > 0) color.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f))
                            .then(if (isTarget && zd.durationMs > 0) Modifier.border(1.dp, color, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)) else Modifier)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(zoneName(zd.zone), color = if (zd.durationMs > 0) color else Color.White.copy(alpha = 0.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    if (sec > 0) Text(formatElapsed(sec), color = Color.White.copy(alpha = 0.3f), fontSize = 7.sp)
                    if (isTarget) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                            Text("TARGET", color = color, fontSize = 6.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Summary table
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.03f)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            distribution.filter { it.durationMs > 0 }.forEach { zd ->
                val color = zoneColor(zd.zone)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                        Text("${zoneName(zd.zone)} · ${zoneFullName(zd.zone)}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        if (zd.zone == targetZone) Text("★", color = color, fontSize = 9.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatElapsed(zd.durationMs / 1000), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("${(zd.percentage * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// RECOVERY VIEW
// ─────────────────────────────────────────────

@Composable
private fun RecoveryView(
    recoveryEvents: List<RecoveryEvent>,
    modifier:       Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (recoveryEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No sprint-recovery data detected.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            }
            return@Column
        }

        val fatigue = recoveryEvents.size >= 2 &&
                recoveryEvents.takeLast(2).all { it.rating == RecoveryRating.POOR }

        if (fatigue) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFBBF24).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("⚠", fontSize = 14.sp)
                Text("Consider rest or lower intensity", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        recoveryEvents.forEachIndexed { idx, ev ->
            val color = ev.rating.color
            val zoneC = zoneColor(ev.peak.zone)

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(color))
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Sprint #${idx + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.12f))
                                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(ev.rating.label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(zoneC.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text(zoneName(ev.peak.zone), color = zoneC, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${ev.peak.bpm} BPM @ ${formatElapsed(ev.peak.elapsedSec)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("↓", color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("${formatElapsed(ev.recoveryTimeSec)} → -${ev.hrDrop} BPM", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Text(
                        "Recovery: -${ev.hrDrop} BPM ${if (ev.hrDrop >= 30) "✓" else if (ev.hrDrop >= 20) "~" else "✗"}",
                        color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE EXISTENTE (păstrate neschimbate)
// ─────────────────────────────────────────────

@Composable
private fun QuickStatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(CardDark)
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White.copy(alpha = 0.25f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, lineHeight = 20.sp)
        if (unit.isNotEmpty()) Text(unit, color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HrChartCard(hrList: List<Int>, maxHr: Int, totalDurationSeconds: Long, secondsPerSample: Long) {
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    val peakHr = if (hrList.isNotEmpty()) hrList.max() else 0
    val peakLactate = hrToLactate(peakHr, maxHr)
    val peakLactateColor = hrToLactateColor(peakHr, maxHr)

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).padding(16.dp)) {
        if (hrList.isNotEmpty()) {
            HrLineChart(hrList, maxHr, totalDurationSeconds, secondsPerSample, tooltipIndex, { tooltipIndex = it }, Modifier.fillMaxWidth().height(200.dp))
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No heart rate data saved", color = Color.Gray, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avg. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text("${if (hrList.isNotEmpty()) hrList.average().toInt() else 0} bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Max. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text("$peakHr bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Peak lactate", color = Color.Gray, fontSize = 12.sp)
                Text(peakLactate, color = peakLactateColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LactateThresholdRow(maxHr)
    }
}

@Composable
private fun LactateThresholdRow(maxHr: Int) {
    val lt1Bpm = (maxHr * 0.70).toInt(); val lt2Bpm = (maxHr * 0.85).toInt()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFBBF24).copy(alpha = 0.07f)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("LT1 · $lt1Bpm bpm", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("~2 mmol/L", color = Color(0xFFFBBF24).copy(alpha = 0.55f), fontSize = 11.sp) }
            InfoIconButton(info = MetricInfoData.LT1, tint = Color(0xFFFBBF24).copy(alpha = 0.5f))
        }
        Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF97316).copy(alpha = 0.07f)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("LT2 · $lt2Bpm bpm", color = Color(0xFFF97316), fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("~4 mmol/L", color = Color(0xFFF97316).copy(alpha = 0.55f), fontSize = 11.sp) }
            InfoIconButton(info = MetricInfoData.LT2, tint = Color(0xFFF97316).copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun HrLineChart(hrList: List<Int>, maxHr: Int, totalDurationSeconds: Long, secondsPerSample: Long, tooltipIndex: Int?, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (hrList.isEmpty()) return
    val lt1Hr = (maxHr * 0.70).toInt(); val lt2Hr = (maxHr * 0.85).toInt()
    val dataMin = hrList.min(); val dataMax = hrList.max()
    val padding = ((dataMax - dataMin) * 0.15f).toInt().coerceAtLeast(5)
    val yMin = (dataMin - padding).coerceAtLeast(0); val yMax = dataMax + padding
    val yRange = (yMax - yMin).toFloat()
    val yLabels = List(5) { i -> yMin + ((yRange / 4f) * (4 - i)).toInt() }
    val lt1Color = Color(0xFFFBBF24); val lt2Color = Color(0xFFF97316)
    val showLt1 = lt1Hr in yMin..yMax; val showLt2 = lt2Hr in yMin..yMax

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(hrList) {
            detectTapGestures { offset ->
                val chartLeft = 48.dp.toPx(); val chartWidth = size.width - chartLeft - 8.dp.toPx()
                onTap(((offset.x - chartLeft) / chartWidth * hrList.size).toInt().coerceIn(0, hrList.size - 1))
            }
        }) {
            val cL = 48.dp.toPx(); val cR = size.width - 8.dp.toPx()
            val cT = 8.dp.toPx(); val cB = size.height - 24.dp.toPx()
            val cW = cR - cL; val cH = cB - cT
            fun hrY(hr: Int) = cB - ((hr - yMin).toFloat() / yRange) * cH
            fun idxX(i: Int) = cL + (i.toFloat() / (hrList.size - 1)) * cW
            val lt1Y = hrY(lt1Hr).coerceIn(cT, cB); val lt2Y = hrY(lt2Hr).coerceIn(cT, cB)
            if (showLt1) drawRect(Color(0xFF60A5FA).copy(alpha = 0.04f), Offset(cL, lt1Y), Size(cW, cB - lt1Y))
            if (showLt1 && showLt2) drawRect(Color(0xFFFBBF24).copy(alpha = 0.04f), Offset(cL, lt2Y), Size(cW, lt1Y - lt2Y))
            if (showLt2) drawRect(Color(0xFFEF4444).copy(alpha = 0.05f), Offset(cL, cT), Size(cW, lt2Y - cT))
            yLabels.forEachIndexed { i, label ->
                val y = cT + (i.toFloat() / (yLabels.size - 1)) * cH
                drawLine(Color.White.copy(alpha = 0.05f), Offset(cL, y), Offset(cR, y), 1f)
                drawContext.canvas.nativeCanvas.drawText("$label", cL - 6.dp.toPx(), y + 4.dp.toPx(), android.graphics.Paint().apply { color = android.graphics.Color.argb(120,255,255,255); textSize = 10.dp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT })
            }
            for (i in 0..4) { val f = i.toFloat()/4; val x = cL+f*cW; val s = (f*totalDurationSeconds).toLong(); drawContext.canvas.nativeCanvas.drawText("%02d:%02d".format(s/60,s%60), x, size.height, android.graphics.Paint().apply { color = android.graphics.Color.argb(100,255,255,255); textSize = 9.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }) }
            val dL = 8.dp.toPx(); val gL = 5.dp.toPx()
            if (showLt1) { var xC = cL; while (xC < cR) { drawLine(lt1Color.copy(alpha=0.45f), Offset(xC,lt1Y), Offset((xC+dL).coerceAtMost(cR),lt1Y), 1.2.dp.toPx()); xC+=dL+gL }; drawContext.canvas.nativeCanvas.drawText("LT1", cL+4.dp.toPx(), lt1Y-3.dp.toPx(), android.graphics.Paint().apply { color=android.graphics.Color.argb(160,251,191,36); textSize=9.dp.toPx(); isFakeBoldText=true }) }
            if (showLt2) { var xC = cL; while (xC < cR) { drawLine(lt2Color.copy(alpha=0.45f), Offset(xC,lt2Y), Offset((xC+dL).coerceAtMost(cR),lt2Y), 1.2.dp.toPx()); xC+=dL+gL }; drawContext.canvas.nativeCanvas.drawText("LT2", cL+4.dp.toPx(), lt2Y-3.dp.toPx(), android.graphics.Paint().apply { color=android.graphics.Color.argb(160,249,115,22); textSize=9.dp.toPx(); isFakeBoldText=true }) }
            val fP = Path().apply { moveTo(idxX(0),cB); lineTo(idxX(0),hrY(hrList[0])); for(i in 1 until hrList.size) lineTo(idxX(i),hrY(hrList[i])); lineTo(idxX(hrList.size-1),cB); close() }
            drawPath(fP, Brush.verticalGradient(colorStops = arrayOf(0f to Color(0xFFEF4444).copy(alpha=0.30f), 0.35f to Color(0xFFF97316).copy(alpha=0.20f), 0.65f to Color(0xFFFBBF24).copy(alpha=0.12f), 1f to Color(0xFF60A5FA).copy(alpha=0.04f)), startY=cT, endY=cB))
            val lP = Path().apply { moveTo(idxX(0),hrY(hrList[0])); for(i in 1 until hrList.size) lineTo(idxX(i),hrY(hrList[i])) }
            drawPath(lP, Color.White.copy(alpha=0.65f), style=Stroke(1.3.dp.toPx(), cap=StrokeCap.Round, join=StrokeJoin.Round))
            tooltipIndex?.let { idx ->
                if (idx < hrList.size) {
                    val x=idxX(idx); val y=hrY(hrList[idx]); val hr=hrList[idx]; val pct=hr.toFloat()/maxHr
                    val tzc = when { pct>=0.90f->android.graphics.Color.argb(255,239,68,68); pct>=0.85f->android.graphics.Color.argb(255,249,115,22); pct>=0.70f->android.graphics.Color.argb(255,251,191,36); else->android.graphics.Color.argb(255,96,165,250) }
                    val lz = when { pct>=0.90f->"> 6 mmol/L"; pct>=0.85f->"~4–6 mmol/L · LT2"; pct>=0.70f->"~2–4 mmol/L · LT1"; else->"< 2 mmol/L · Recovery" }
                    val sec=idx*secondsPerSample; val l1="$hr bpm  ${"%02d:%02d".format(sec/60,sec%60)}"
                    val p1=android.graphics.Paint().apply{textSize=11.dp.toPx();color=android.graphics.Color.WHITE;isFakeBoldText=true}
                    val p2=android.graphics.Paint().apply{textSize=10.dp.toPx();color=tzc}
                    val bW=maxOf(p1.measureText(l1),p2.measureText(lz))+20.dp.toPx(); val bH=29.dp.toPx()
                    val bL=(x-bW/2f).coerceIn(cL,cR-bW); val bT=(y-bH-12.dp.toPx()).coerceAtLeast(cT+4.dp.toPx())
                    drawLine(Color.White.copy(alpha=0.2f),Offset(x,cT),Offset(x,cB),1f)
                    drawCircle(Color.White,5.dp.toPx(),Offset(x,y)); drawCircle(when{pct>=0.90f->Color(0xFFEF4444);pct>=0.85f->Color(0xFFF97316);pct>=0.70f->Color(0xFFFBBF24);else->Color(0xFF60A5FA)},3.dp.toPx(),Offset(x,y))
                    drawRoundRect(Color(0xF0111116),Offset(bL,bT),Size(bW,bH),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha=0.08f),Offset(bL,bT),Size(bW,bH),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),style=Stroke(1f))
                    drawContext.canvas.nativeCanvas.drawText(l1,bL+10.dp.toPx(),bT+6.dp.toPx()+11.dp.toPx(),p1)
                    drawContext.canvas.nativeCanvas.drawText(lz,bL+10.dp.toPx(),bT+6.dp.toPx()+11.dp.toPx()+12.dp.toPx(),p2)
                }
            }
        }
    }
}

@Composable
fun ZoneBreakdownSection(hrList: List<Int>, maxHr: Int, secondsPerSample: Long) {
    val totalSamples = hrList.size.toFloat()
    val z5 = hrList.count { hrToZone(it,maxHr)==5 }; val z4 = hrList.count { hrToZone(it,maxHr)==4 }
    val z3 = hrList.count { hrToZone(it,maxHr)==3 }; val z2 = hrList.count { hrToZone(it,maxHr)==2 }
    val z1 = hrList.count { hrToZone(it,maxHr)==1 }
    Column {
        ZoneItem("Zone 5: Maximum","${(maxHr*0.9).toInt()}–$maxHr bpm",z5,totalSamples,secondsPerSample,Zone5Color,5)
        ZoneDivider()
        ZoneItem("Zone 4: Anaerobic","${(maxHr*0.8).toInt()}–${(maxHr*0.9-1).toInt()} bpm",z4,totalSamples,secondsPerSample,Zone4Color,4)
        ZoneDivider()
        ZoneItem("Zone 3: Aerobic","${(maxHr*0.7).toInt()}–${(maxHr*0.8-1).toInt()} bpm",z3,totalSamples,secondsPerSample,Zone3Color,3)
        ZoneDivider()
        ZoneItem("Zone 2: Weight control","${(maxHr*0.6).toInt()}–${(maxHr*0.7-1).toInt()} bpm",z2,totalSamples,secondsPerSample,Zone2Color,2)
        ZoneDivider()
        ZoneItem("Zone 1: Low intensity","sub ${(maxHr*0.6).toInt()} bpm",z1,totalSamples,secondsPerSample,Zone1Color,1)
    }
}

@Composable
private fun ZoneDivider() { HorizontalDivider(color = Color.White.copy(alpha=0.05f), thickness=0.5.dp, modifier=Modifier.padding(vertical=2.dp)) }

@SuppressLint("DefaultLocale")
@Composable
private fun ZoneItem(title: String, rangeText: String, count: Int, totalCount: Float, secondsPerSample: Long, color: Color, zoneNumber: Int = 0) {
    val percentage = if (totalCount > 0) count/totalCount else 0f
    val totalSeconds = count * secondsPerSample
    val timeString = String.format("%02d:%02d", totalSeconds/60, totalSeconds%60)
    val zoneInfo = when(zoneNumber){5->MetricInfoData.ZONE_5;4->MetricInfoData.ZONE_4;3->MetricInfoData.ZONE_3;2->MetricInfoData.ZONE_2;1->MetricInfoData.ZONE_1;else->null}
    Column(modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)) {
        Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Row(verticalAlignment=Alignment.CenterVertically) { Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp); zoneInfo?.let{InfoIconButton(info=it,tint=Color.White.copy(alpha=0.25f))} }
            Text(rangeText,color=Color.Gray,fontSize=13.sp)
        }
        Spacer(modifier=Modifier.height(8.dp))
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(timeString,color=Color.Gray,fontSize=13.sp,modifier=Modifier.width(46.dp))
            Box(modifier=Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha=0.07f))) { if(percentage>0f) Box(modifier=Modifier.fillMaxWidth(percentage.coerceAtLeast(0.01f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color)) }
            Text("${(percentage*100).toInt()}%",color=Color.Gray,fontSize=13.sp,modifier=Modifier.width(36.dp))
        }
    }
}

@Composable
private fun AthleticImpactSection(session: TrainingSessionEntity) {
    val intensity = when { session.finalTrimp>80||session.cnsScoreAtEnd<40->"Hard"; session.finalTrimp>40||session.cnsScoreAtEnd<60->"Medium"; else->"Easy" }
    val pts = when(intensity){"Hard"->3f;"Medium"->2f;else->1f}
    data class AxisChange(val label:String,val delta:Float,val positive:Boolean)
    val changes = mutableListOf<AxisChange>()
    when(session.type.uppercase()){
        "STRENGTH"->{changes.add(AxisChange("Strength",pts,true));val eB=when(session.activityType.lowercase()){"bodyweight","calisthenics"->pts*0.15f;else->0f};if(eB>0)changes.add(AxisChange("Endurance",eB,true));changes.add(AxisChange("HRR",if(pts>=3f)1.5f else 0.5f,true))}
        "ENDURANCE"->{changes.add(AxisChange("Endurance",pts,true));val sB=when(session.activityType.lowercase()){"bag work"->pts*0.10f;else->0f};if(sB>0)changes.add(AxisChange("Speed",sB,true));changes.add(AxisChange("HRR",if(pts>=3f)1.5f else 0.5f,true))}
        "SPEED"->{val(sP,stB,eB)=when(session.activityType.lowercase()){"martial arts","boxing"->Triple(pts*0.6f,pts*0.2f,pts*0.2f);"intervals"->Triple(pts*0.7f,0f,pts*0.3f);"agility"->Triple(pts*0.8f,pts*0.1f,pts*0.1f);else->Triple(pts,0f,0f)};changes.add(AxisChange("Speed",sP,true));if(stB>0)changes.add(AxisChange("Strength",stB,true));if(eB>0)changes.add(AxisChange("Endurance",eB,true));changes.add(AxisChange("HRR",if(pts>=3f)1.5f else 0.5f,true))}
        "RECOVERY","REST"->changes.add(AxisChange("HRR",0.3f,true))
    }
    val rpeNote=when{session.rpe in 7..8->" · Optimal effort";session.rpe>=9->" · High effort";session.rpe in 1..3->" · Low effort";else->""}
    val activityNote=if(session.activityType.isNotEmpty())" · ${session.activityType}" else ""
    Column(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111118)).border(1.dp,Color.White.copy(alpha=0.08f),RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("ATHLETIC IMPACT",color=Color.White.copy(alpha=0.25f),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,modifier=Modifier.padding(bottom=10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(bottom=10.dp)){changes.forEach{change->val color=if(change.positive)Color(0xFF4ADE80) else Color(0xFFF87171);Box(modifier=Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha=0.1f)).border(1.dp,color.copy(alpha=0.25f),RoundedCornerShape(20.dp)).padding(horizontal=12.dp,vertical=5.dp)){Text("+${"%.1f".format(change.delta)} ${change.label}",color=color,fontSize=12.sp,fontWeight=FontWeight.Bold)}}}
        Text("$intensity session$activityNote$rpeNote",color=Color.White.copy(alpha=0.3f),fontSize=12.sp,lineHeight=18.sp)
    }
}