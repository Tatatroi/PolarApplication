package com.application.polarapplication.athletic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.planning.TrainingPlanner
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgDark      = Color(0xFF080808)
private val CardDark    = Color(0xFF111118)
private val CardBorder  = Color(0x17FFFFFF)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentRed    = Color(0xFFF87171)
private val AccentAmber  = Color(0xFFFBBF24)
private val AccentBlue   = Color(0xFF60A5FA)

private fun plannedTrimp(type: WorkoutType): Double = when (type) {
    WorkoutType.STRENGTH  -> 60.0
    WorkoutType.ENDURANCE -> 80.0
    WorkoutType.SPEED     -> 50.0
    WorkoutType.RECOVERY  -> 20.0
    WorkoutType.REST      -> 0.0
}

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: DashboardViewModel = viewModel(),
    onBack: () -> Unit
) {
    val allSessions     by viewModel.allSessions.collectAsState()
    val competitionDate by viewModel.competitionDate.collectAsState()
    val planStartDate   by viewModel.planStartDate.collectAsState()
    val scoreHistory    by viewModel.athleticProfileManager.scoreHistory.collectAsState()
    val scores          by viewModel.athleticProfileManager.scores.collectAsState()

    val today         = remember { LocalDate.now() }
    val thirtyDaysAgo = remember { today.minusDays(30) }

    val recentSessions = remember(allSessions) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        allSessions.filter { it.date >= cutoff }
    }

    val planner       = remember { TrainingPlanner() }
    val effectiveStart = planStartDate ?: today
    val effectiveComp  = competitionDate ?: today.plusWeeks(24)
    val plan = remember(effectiveStart, effectiveComp) {
        planner.generatePlan(effectiveComp, effectiveStart)
    }

    val trimpData = remember(recentSessions, plan) {
        buildTrImpData(recentSessions, plan, thirtyDaysAgo, today)
    }

    val totalMinutes = remember(recentSessions) {
        recentSessions.sumOf { it.durationSeconds } / 60
    }

    val cnsData = remember(recentSessions) {
        buildCnsData(recentSessions, thirtyDaysAgo, today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White.copy(alpha = 0.6f))
            }
            Column {
                Text("Progress", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Last 30 days · Bompa plan comparison",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ── SECTION 1: Training Load ──────────────────────────────────────
            SectionHeader(
                icon       = Icons.Default.FitnessCenter,
                title      = "Training Load",
                subtitle   = "How hard did you train?",
                color      = AccentIndigo
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Explanation card
            ExplanationCard(
                text  = "Training Load combines how long you trained and how hard your heart worked. " +
                        "Higher = more effort. The dashed line shows what your plan recommended.",
                color = AccentIndigo
            )
            Spacer(modifier = Modifier.height(10.dp))

            TrImpChart(data = trimpData, totalMinutes = totalMinutes)

            // ── DIVIDER ───────────────────────────────────────────────────────
            SectionDivider()

            // ── SECTION 2: CNS Recovery ───────────────────────────────────────
            SectionHeader(
                icon     = Icons.Default.MonitorHeart,
                title    = "CNS Recovery",
                subtitle = "How recovered is your nervous system?",
                color    = AccentGreen
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExplanationCard(
                text  = "Your Central Nervous System (CNS) controls muscle activation. " +
                        "Below 70% means you may feel sluggish. Red zones = days you trained tired.",
                color = AccentGreen
            )
            Spacer(modifier = Modifier.height(10.dp))

            CnsChart(data = cnsData)

            // ── DIVIDER ───────────────────────────────────────────────────────
            SectionDivider()

            // ── SECTION 3: Fitness Forecast ───────────────────────────────────
            SectionHeader(
                icon     = Icons.Default.TrendingUp,
                title    = "Fitness Forecast",
                subtitle = "Where you're headed by competition day",
                color    = AccentAmber
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExplanationCard(
                text  = "White line = your actual progress after each session. " +
                        "Dashed line = where you should be following the Bompa plan. " +
                        "Stay close to the dashed line for best results.",
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 3 grafice forecast
            ForecastProgressChart(
                title           = "Strength",
                icon            = Icons.Default.FitnessCenter,
                accentColor     = AccentIndigo,
                currentScore    = scores.strength,
                scoreHistory    = scoreHistory,
                axis            = "strength",
                competitionDate = effectiveComp,
                getValue        = { it.strength }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ForecastProgressChart(
                title           = "Speed",
                icon            = Icons.Default.Speed,
                accentColor     = AccentAmber,
                currentScore    = scores.speed,
                scoreHistory    = scoreHistory,
                axis            = "speed",
                competitionDate = effectiveComp,
                getValue        = { it.speed }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ForecastProgressChart(
                title           = "Endurance",
                icon            = Icons.Default.DirectionsRun,
                accentColor     = AccentGreen,
                currentScore    = scores.endurance,
                scoreHistory    = scoreHistory,
                axis            = "endurance",
                competitionDate = effectiveComp,
                getValue        = { it.endurance }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    color:    Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────
// EXPLANATION CARD
// ─────────────────────────────────────────────

@Composable
private fun ExplanationCard(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint     = color.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Text(
            text,
            color      = Color.White.copy(alpha = 0.45f),
            fontSize   = 11.sp,
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────
// SECTION DIVIDER
// ─────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

// ─────────────────────────────────────────────
// TRIMP CHART
// ─────────────────────────────────────────────

@Composable
private fun TrImpChart(data: List<DayPoint>, totalMinutes: Long) {
    val totalActual  = data.sumOf { it.actual }
    val totalPlanned = data.sumOf { it.planned }
    val pct = if (totalPlanned > 0)
        ((totalActual / totalPlanned - 1) * 100).toInt() else 0
    val pctColor = if (pct >= -10) AccentGreen else AccentRed
    val pctText  = if (pct >= 0) "+$pct% vs plan" else "$pct% vs plan"

    val avgActual  = if (data.isNotEmpty()) totalActual / (data.size / 7.0) else 0.0
    val avgPlanned = if (data.isNotEmpty()) totalPlanned / (data.size / 7.0) else 0.0

    ChartCard {
        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier              = Modifier.padding(bottom = 8.dp)
        ) {
            LegendLine(color = Color.White.copy(alpha = 0.7f), label = "Actual load",  dashed = false)
            LegendLine(color = AccentIndigo,                    label = "Planned load", dashed = true)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            if (data.isEmpty()) return@Canvas
            drawTrImpLines(data)
        }

        // Stats row
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatBox(
                label = "Avg/week",
                value = "${"%.0f".format(avgActual)}",
                sub   = "actual",
                color = AccentIndigo,
                modifier = Modifier.weight(1f)
            )
            MiniStatBox(
                label = "Avg/week",
                value = "${"%.0f".format(avgPlanned)}",
                sub   = "planned",
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatBox(
                label = "vs Plan",
                value = if (pct >= 0) "+$pct%" else "$pct%",
                sub   = if (pct >= -10) "on track" else "behind",
                color = pctColor,
                modifier = Modifier.weight(1f)
            )
            MiniStatBox(
                label = "Total time",
                value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                sub   = "last 30 days",
                color = AccentBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// CNS CHART
// ─────────────────────────────────────────────

@Composable
private fun CnsChart(data: List<CnsPoint>) {
    val avgCns     = if (data.isNotEmpty()) data.map { it.cns }.average().toInt() else 0
    val belowCount = data.count { it.cns < 70 }
    val trend = if (data.size >= 7) {
        val last7 = data.takeLast(7).map { it.cns }.average()
        val prev7 = data.dropLast(7).takeLast(7).map { it.cns }.average()
        when {
            last7 > prev7 + 3 -> "↑ Improving"
            last7 < prev7 - 3 -> "↓ Declining"
            else              -> "→ Stable"
        }
    } else "→ Stable"
    val trendColor = when {
        trend.startsWith("↑") -> AccentGreen
        trend.startsWith("↓") -> AccentRed
        else                  -> AccentAmber
    }

    ChartCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier              = Modifier.padding(bottom = 8.dp)
        ) {
            LegendLine(color = AccentGreen, label = "CNS score", dashed = false)
            LegendLine(color = AccentGreen.copy(alpha = 0.4f), label = "Optimal threshold (70)", dashed = true)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            if (data.isEmpty()) return@Canvas
            drawCnsChart(data)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatBox(
                label = "Average",
                value = "$avgCns%",
                sub   = if (avgCns >= 70) "good" else "low",
                color = if (avgCns >= 70) AccentGreen else AccentAmber,
                modifier = Modifier.weight(1f)
            )
            MiniStatBox(
                label = "Tired days",
                value = "$belowCount",
                sub   = "below 70%",
                color = if (belowCount > 5) AccentRed else AccentAmber,
                modifier = Modifier.weight(1f)
            )
            MiniStatBox(
                label = "Trend",
                value = trend.take(1),
                sub   = trend.drop(2),
                color = trendColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// FORECAST PROGRESS CHART (Strength/Speed/End)
// ─────────────────────────────────────────────

@Composable
private fun ForecastProgressChart(
    title:           String,
    icon:            ImageVector,
    accentColor:     Color,
    currentScore:    Float,
    scoreHistory:    List<ScoreSnapshot>,
    axis:            String,
    competitionDate: LocalDate,
    getValue:        (ScoreSnapshot) -> Float
) {
    val actualPoints = remember(scoreHistory) {
        scoreHistory.map { snap ->
            Instant.ofEpochMilli(snap.timestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate() to getValue(snap)
        }.sortedBy { it.first }
    }

    val predictedPoints = remember(currentScore, competitionDate, axis) {
        generatePredictedCurve(currentScore, axis, competitionDate)
    }

    val currentInt    = currentScore.toInt()
    val predictedAtComp = predictedPoints.lastOrNull()?.second?.toInt() ?: currentInt
    val gain          = predictedAtComp - currentInt
    val compFmt       = DateTimeFormatter.ofPattern("dd MMM")

    // Stato: se no dati
    val hasData = actualPoints.isNotEmpty() || currentScore > 0

    ChartCard {
        // Header con icona + score
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            // Score now → comp
            Row(
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "$currentInt",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "→ $predictedAtComp",
                    color = accentColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
                Text(
                    if (gain >= 0) "(+$gain)" else "($gain)",
                    color    = if (gain >= 0) accentColor else AccentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendLine(color = Color.White.copy(alpha = 0.7f), label = "Your progress", dashed = false)
            LegendLine(color = Color(0xFF818CF8), label = "Bompa target", dashed = true)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasData) {
            // Placeholder se non ci sono dati
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BarChart,
                        null,
                        tint     = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Complete the athletic test to see your $title progress",
                        color    = Color.White.copy(alpha = 0.2f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Chart
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                ForecastLineChart(
                    actualPoints    = actualPoints,
                    predictedPoints = predictedPoints,
                    accentColor     = accentColor,
                    modifier        = Modifier.fillMaxSize()
                )
            }

            // X labels
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp)
                Text(
                    "Competition · ${competitionDate.format(compFmt)}",
                    color    = AccentRed.copy(alpha = 0.5f),
                    fontSize = 8.sp
                )
            }

            // Phase bar
            Spacer(modifier = Modifier.height(8.dp))
            PhaseBar(competitionDate = competitionDate)
        }
    }
}

// ─────────────────────────────────────────────
// FORECAST LINE CHART CANVAS
// ─────────────────────────────────────────────

@Composable
private fun ForecastLineChart(
    actualPoints:    List<Pair<LocalDate, Float>>,
    predictedPoints: List<Pair<LocalDate, Float>>,
    accentColor:     Color,
    modifier:        Modifier = Modifier
) {
    val allDates  = (actualPoints.map { it.first } + predictedPoints.map { it.first }).sorted()
    val minDate   = allDates.firstOrNull() ?: LocalDate.now()
    val maxDate   = allDates.lastOrNull() ?: LocalDate.now().plusDays(1)
    val totalDays = ChronoUnit.DAYS.between(minDate, maxDate).toFloat().coerceAtLeast(1f)

    val allVals = (actualPoints.map { it.second } + predictedPoints.map { it.second })
    val minVal  = 0f
    val maxVal  = (allVals.maxOrNull() ?: 100f).coerceAtLeast(10f)
    val range   = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w       = size.width
        val h       = size.height
        val padTop  = 8.dp.toPx()
        val padBot  = 8.dp.toPx()
        val chartH  = h - padTop - padBot

        fun dateX(d: LocalDate) =
            (ChronoUnit.DAYS.between(minDate, d).toFloat() / totalDays) * w

        fun valY(v: Float) =
            padTop + chartH - ((v - minVal) / range) * chartH

        // Grid leggero
        listOf(25f, 50f, 75f, 100f).forEach { gv ->
            val gy = valY(gv)
            if (gy in padTop..h - padBot) {
                drawLine(
                    Color.White.copy(alpha = 0.04f),
                    Offset(0f, gy), Offset(w, gy), 1f
                )
            }
        }

        // Predicted fill
        if (predictedPoints.size >= 2) {
            val fill = Path().apply {
                moveTo(dateX(predictedPoints.first().first), h - padBot)
                predictedPoints.forEach { (d, v) -> lineTo(dateX(d), valY(v)) }
                lineTo(dateX(predictedPoints.last().first), h - padBot)
                close()
            }
            drawPath(fill, Brush.verticalGradient(
                listOf(Color(0xFF818CF8).copy(alpha = 0.08f), Color.Transparent)
            ))
        }

        // Predicted line tratteggiata
        if (predictedPoints.size >= 2) {
            val path = Path().apply {
                predictedPoints.forEachIndexed { i, (d, v) ->
                    if (i == 0) moveTo(dateX(d), valY(v)) else lineTo(dateX(d), valY(v))
                }
            }
            drawPath(path, Color(0xFF818CF8).copy(alpha = 0.6f),
                style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
            // Dot finale
            val lp = predictedPoints.last()
            drawCircle(Color(0xFF818CF8), 4.dp.toPx(), Offset(dateX(lp.first), valY(lp.second)))
            drawCircle(Color(0xFF818CF8).copy(alpha = 0.25f), 7.dp.toPx(),
                Offset(dateX(lp.first), valY(lp.second)), style = Stroke(1.5.dp.toPx()))
        }

        // Actual fill
        if (actualPoints.size >= 2) {
            val fill = Path().apply {
                moveTo(dateX(actualPoints.first().first), h - padBot)
                actualPoints.forEach { (d, v) -> lineTo(dateX(d), valY(v)) }
                lineTo(dateX(actualPoints.last().first), h - padBot)
                close()
            }
            drawPath(fill, Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.2f), Color.Transparent)
            ))
        }

        // Actual line
        if (actualPoints.size >= 2) {
            val path = Path().apply {
                actualPoints.forEachIndexed { i, (d, v) ->
                    if (i == 0) moveTo(dateX(d), valY(v)) else lineTo(dateX(d), valY(v))
                }
            }
            drawPath(path, Color.White.copy(alpha = 0.85f),
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            actualPoints.forEach { (d, v) ->
                drawCircle(Color.White.copy(alpha = 0.5f), 2.5.dp.toPx(), Offset(dateX(d), valY(v)))
            }
            val la = actualPoints.last()
            drawCircle(accentColor, 4.dp.toPx(), Offset(dateX(la.first), valY(la.second)))
        } else if (actualPoints.size == 1) {
            val (d, v) = actualPoints.first()
            drawCircle(accentColor, 5.dp.toPx(), Offset(dateX(d), valY(v)))
        }

        // Linea oggi
        val todayX = dateX(LocalDate.now()).coerceIn(0f, w)
        drawLine(Color.White.copy(alpha = 0.12f), Offset(todayX, padTop), Offset(todayX, h - padBot),
            1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    }
}

// ─────────────────────────────────────────────
// PHASE BAR
// ─────────────────────────────────────────────

@Composable
private fun PhaseBar(competitionDate: LocalDate) {
    val phases = listOf(
        "General"  to (Color(0xFF4ADE80) to 0.40f),
        "Specific" to (Color(0xFFFBBF24) to 0.30f),
        "Pre-Comp" to (Color(0xFFA78BFA) to 0.20f),
        "Comp"     to (Color(0xFFF87171) to 0.05f),
        "Recovery" to (Color(0xFF60A5FA) to 0.05f)
    )
    Column {
        Text(
            "Bompa phases until competition",
            color    = Color.White.copy(alpha = 0.15f),
            fontSize = 8.sp,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            phases.forEach { (_, pair) ->
                Box(
                    modifier = Modifier
                        .weight(pair.second)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(pair.first.copy(alpha = 0.45f))
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            phases.forEach { (label, pair) ->
                Box(modifier = Modifier.weight(pair.second)) {
                    if (pair.second >= 0.15f) {
                        Text(
                            label,
                            color      = pair.first.copy(alpha = 0.5f),
                            fontSize   = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PREDICTED CURVE ENGINE
// ─────────────────────────────────────────────

private fun generatePredictedCurve(
    startScore:      Float,
    axis:            String,
    competitionDate: LocalDate,
    pointCount:      Int = 20
): List<Pair<LocalDate, Float>> {
    val today     = LocalDate.now()
    val totalDays = ChronoUnit.DAYS.between(today, competitionDate).toInt().coerceAtLeast(7)

    val phases = listOf(
        "general"  to 0.40, "specific" to 0.30,
        "precomp"  to 0.20, "comp"     to 0.05, "recovery" to 0.05
    )

    fun rate(phase: String) = when (axis.lowercase()) {
        "strength" -> when (phase) {
            "general" -> 0.18f; "specific" -> 0.35f; "precomp" -> 0.22f
            "comp"    -> 0.04f; else       -> -0.08f
        }
        "speed" -> when (phase) {
            "general" -> 0.08f; "specific" -> 0.28f; "precomp" -> 0.38f
            "comp"    -> 0.04f; else       -> -0.08f
        }
        "endurance" -> when (phase) {
            "general" -> 0.32f; "specific" -> 0.18f; "precomp" -> 0.08f
            "comp"    -> 0.04f; else       -> -0.05f
        }
        else -> 0f
    }

    val daily = mutableListOf<Pair<LocalDate, Float>>()
    var score = startScore
    var day   = 0

    for ((phase, pct) in phases) {
        val phaseDays = (totalDays * pct).toInt().coerceAtLeast(1)
        val r = rate(phase)
        repeat(phaseDays) {
            score = (score + r).coerceIn(0f, 100f)
            daily.add(today.plusDays(day.toLong()) to score)
            day++
        }
    }

    if (daily.size <= pointCount) return daily
    val step = daily.size / pointCount
    return daily.filterIndexed { i, _ -> i % step == 0 }
}

// ─────────────────────────────────────────────
// DATA BUILDERS
// ─────────────────────────────────────────────

data class DayPoint(val date: LocalDate, val actual: Double, val planned: Double)
data class CnsPoint(val date: LocalDate, val cns: Int)

private fun buildTrImpData(
    sessions: List<TrainingSessionEntity>,
    plan:     com.application.polarapplication.ai.model.TrainingPlan,
    from:     LocalDate,
    to:       LocalDate
): List<DayPoint> {
    val days = ChronoUnit.DAYS.between(from, to).toInt()
    return (0..days).map { offset ->
        val date = from.plusDays(offset.toLong())
        val actual = sessions
            .filter { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == date }
            .sumOf { it.finalTrimp }
        val micro = plan.mesoCycles.flatMap { it.microCycle }
            .firstOrNull { !it.startDate.isAfter(date) && !it.endDate.isBefore(date) }
        val wi = (date.dayOfWeek.value - 1).coerceIn(0, 6)
        val wt = micro?.workouts?.getOrNull(wi) ?: WorkoutType.REST
        DayPoint(date, actual, plannedTrimp(wt))
    }
}

private fun buildCnsData(
    sessions: List<TrainingSessionEntity>,
    from:     LocalDate,
    to:       LocalDate
): List<CnsPoint> {
    val days = ChronoUnit.DAYS.between(from, to).toInt()
    return (0..days).mapNotNull { offset ->
        val date = from.plusDays(offset.toLong())
        sessions
            .filter { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == date }
            .maxByOrNull { it.date }
            ?.let { CnsPoint(date, it.cnsScoreAtEnd) }
    }
}

// ─────────────────────────────────────────────
// CANVAS DRAW — TRIMP
// ─────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrImpLines(data: List<DayPoint>) {
    val cL = 40.dp.toPx(); val cR = size.width - 8.dp.toPx()
    val cT = 8.dp.toPx();  val cB = size.height - 20.dp.toPx()
    val cW = cR - cL;      val cH = cB - cT
    val maxV = (data.maxOf { maxOf(it.actual, it.planned) } * 1.2).coerceAtLeast(10.0)

    fun vToY(v: Double) = cB - ((v / maxV) * cH).toFloat()
    fun iToX(i: Int)    = cL + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * cW

    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { lvl ->
        val y = cB - lvl * cH
        drawLine(Color.White.copy(alpha = 0.05f), Offset(cL, y), Offset(cR, y), 1f)
        drawContext.canvas.nativeCanvas.drawText(
            "${"%.0f".format(maxV * lvl)}",
            cL - 5.dp.toPx(), y + 4.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(70, 255, 255, 255)
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
        )
    }

    // Planned dashed
    val dLen = 8.dp.toPx(); val gLen = 5.dp.toPx()
    for (i in 0 until data.size - 1) {
        val x1 = iToX(i); val y1 = vToY(data[i].planned)
        val x2 = iToX(i+1); val y2 = vToY(data[i+1].planned)
        val sL = kotlin.math.sqrt(((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)).toDouble()).toFloat()
        val dx = (x2-x1)/sL; val dy = (y2-y1)/sL
        var cur = 0f; var draw = true
        while (cur < sL) {
            val end = (cur + if (draw) dLen else gLen).coerceAtMost(sL)
            if (draw) drawLine(AccentIndigo.copy(alpha = 0.5f),
                Offset(x1+dx*cur, y1+dy*cur), Offset(x1+dx*end, y1+dy*end), 1.5.dp.toPx())
            cur += if (draw) dLen else gLen; draw = !draw
        }
    }

    // Actual fill
    val fill = Path().apply {
        moveTo(iToX(0), cB); lineTo(iToX(0), vToY(data[0].actual))
        for (i in 1 until data.size) lineTo(iToX(i), vToY(data[i].actual))
        lineTo(iToX(data.size-1), cB); close()
    }
    drawPath(fill, Brush.verticalGradient(
        listOf(AccentIndigo.copy(alpha = 0.25f), AccentIndigo.copy(alpha = 0f)),
        startY = cT, endY = cB
    ))

    // Actual line
    val line = Path().apply {
        moveTo(iToX(0), vToY(data[0].actual))
        for (i in 1 until data.size) lineTo(iToX(i), vToY(data[i].actual))
    }
    drawPath(line, Color.White.copy(alpha = 0.8f),
        style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

    for (i in data.indices step 7)
        drawCircle(AccentIndigo, 3.dp.toPx(), Offset(iToX(i), vToY(data[i].actual)))
}

// ─────────────────────────────────────────────
// CANVAS DRAW — CNS
// ─────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCnsChart(data: List<CnsPoint>) {
    val cL = 40.dp.toPx(); val cR = size.width - 8.dp.toPx()
    val cT = 8.dp.toPx();  val cB = size.height - 20.dp.toPx()
    val cW = cR - cL;      val cH = cB - cT

    fun cToY(c: Int) = cB - (c / 100f) * cH
    fun iToX(i: Int) = cL + (i.toFloat() / (data.size-1).coerceAtLeast(1)) * cW

    val t70 = cToY(70)
    listOf(25, 50, 70, 100).forEach { v ->
        val y = cToY(v)
        drawLine(
            if (v == 70) AccentGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
            Offset(cL, y), Offset(cR, y), 1f,
            pathEffect = if (v == 70) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
        )
        drawContext.canvas.nativeCanvas.drawText("$v",
            cL - 5.dp.toPx(), y + 4.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(70, 255, 255, 255)
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            })
    }

    for (i in 0 until data.size - 1) {
        if (data[i].cns < 70 && data[i+1].cns < 70) {
            val rp = Path().apply {
                moveTo(iToX(i), t70); lineTo(iToX(i), cToY(data[i].cns))
                lineTo(iToX(i+1), cToY(data[i+1].cns)); lineTo(iToX(i+1), t70); close()
            }
            drawPath(rp, AccentRed.copy(alpha = 0.10f))
        }
    }

    val fill = Path().apply {
        moveTo(iToX(0), cB); lineTo(iToX(0), cToY(data[0].cns))
        for (i in 1 until data.size) lineTo(iToX(i), cToY(data[i].cns))
        lineTo(iToX(data.size-1), cB); close()
    }
    drawPath(fill, Brush.verticalGradient(
        listOf(AccentGreen.copy(alpha = 0.3f), Color.Transparent), startY = cT, endY = cB))

    val lp = Path().apply {
        moveTo(iToX(0), cToY(data[0].cns))
        for (i in 1 until data.size) lineTo(iToX(i), cToY(data[i].cns))
    }
    drawPath(lp, AccentGreen.copy(alpha = 0.9f),
        style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ─────────────────────────────────────────────
// SHARED UI COMPONENTS
// ─────────────────────────────────────────────

@Composable
private fun ChartCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun LegendLine(color: Color, label: String, dashed: Boolean = false) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Canvas(modifier = Modifier.width(18.dp).height(2.dp)) {
            val pe = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 3f)) else null
            drawLine(color, Offset(0f, size.height/2), Offset(size.width, size.height/2),
                2.dp.toPx(), pathEffect = pe)
        }
        Text(label, color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp)
    }
}

@Composable
private fun MiniStatBox(
    label:    String,
    value:    String,
    sub:      String,
    color:    Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 18.sp)
        Text(sub, color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp)
    }
}