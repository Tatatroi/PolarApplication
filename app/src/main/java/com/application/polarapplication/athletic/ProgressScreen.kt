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
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.geometry.CornerRadius
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.geometry.Size
    import androidx.compose.ui.graphics.*
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.application.polarapplication.ai.daily.WorkoutType
    import com.application.polarapplication.ai.planning.TrainingPlanner
    import com.application.polarapplication.athletic.ScoreSnapshot
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
    private val BgDark       = Color(0xFF080808)
    private val CardDark     = Color(0xFF111118)
    private val CardBorder   = Color(0x17FFFFFF)
    private val AccentIndigo = Color(0xFF818CF8)
    private val AccentGreen  = Color(0xFF4ADE80)
    private val AccentRed    = Color(0xFFF87171)
    private val AccentAmber  = Color(0xFFFBBF24)
    private val AccentBlue   = Color(0xFF60A5FA)

    // TRIMP estimat per tip antrenament
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
        val allSessions      by viewModel.allSessions.collectAsState()
        val competitionDate  by viewModel.competitionDate.collectAsState()
        val planStartDate    by viewModel.planStartDate.collectAsState()
        val scoreHistory     by viewModel.athleticProfileManager.scoreHistory.collectAsState()
        val today            = remember { LocalDate.now() }
        val thirtyDaysAgo    = remember { today.minusDays(30) }

        // Sesiuni din ultimele 30 zile
        val recentSessions = remember(allSessions) {
            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            allSessions.filter { it.date >= cutoff }
        }

        // Plan Bompa pentru TRIMP planificat
        val planner      = remember { TrainingPlanner() }
        val effectiveStart = planStartDate ?: today
        val effectiveComp  = competitionDate ?: today.plusWeeks(24)
        val plan         = remember(effectiveStart, effectiveComp) {
            planner.generatePlan(effectiveComp, effectiveStart)
        }

        // Construim datele pentru grafice
        val trimpData    = remember(recentSessions, plan) {
            buildTrImpData(recentSessions, plan, thirtyDaysAgo, today)
        }
        val cnsData      = remember(recentSessions) {
            buildCnsData(recentSessions, thirtyDaysAgo, today)
        }
        val scoreData    = remember(scoreHistory) {
            scoreHistory.filter { snapshot ->
                val date = Instant.ofEpochMilli(snapshot.timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(thirtyDaysAgo)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
                Column {
                    Text(
                        "Progress",
                        color      = Color.White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Last 30 days · Bompa plan vs actual",
                        color    = Color.White.copy(alpha = 0.3f),
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
                // ── TRIMP Chart ───────────────────────────────────────────────────
                TrImpChart(data = trimpData)

                Spacer(modifier = Modifier.height(12.dp))

                // ── CNS Chart ─────────────────────────────────────────────────────
                CnsChart(data = cnsData)

                Spacer(modifier = Modifier.height(12.dp))

                // ── Athletic Evolution Chart ───────────────────────────────────────
                AthleticEvolutionChart(snapshots = scoreData)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ─────────────────────────────────────────────
    // DATA BUILDERS
    // ─────────────────────────────────────────────

    data class DayPoint(val date: LocalDate, val actual: Double, val planned: Double)
    data class CnsPoint(val date: LocalDate, val cns: Int)

    private fun buildTrImpData(
        sessions: List<TrainingSessionEntity>,
        plan: com.application.polarapplication.ai.model.TrainingPlan,
        from: LocalDate,
        to: LocalDate
    ): List<DayPoint> {
        val days = ChronoUnit.DAYS.between(from, to).toInt()

        return (0..days).map { offset ->
            val date = from.plusDays(offset.toLong())

            // TRIMP real din sesiunile zilei
            val actualTrimp = sessions
                .filter { session ->
                    Instant.ofEpochMilli(session.date)
                        .atZone(ZoneId.systemDefault()).toLocalDate() == date
                }
                .sumOf { it.finalTrimp }

            // TRIMP planificat din planul Bompa
            val micro = plan.mesoCycles
                .flatMap { it.microCycle }
                .firstOrNull { !it.startDate.isAfter(date) && !it.endDate.isBefore(date) }
            val dayIndex     = (date.dayOfWeek.value - 1).coerceIn(0, 6)
            val workoutType  = micro?.workouts?.getOrNull(dayIndex) ?: WorkoutType.REST
            val plannedTrimp = plannedTrimp(workoutType)

            DayPoint(date, actualTrimp, plannedTrimp)
        }
    }

    private fun buildCnsData(
        sessions: List<TrainingSessionEntity>,
        from: LocalDate,
        to: LocalDate
    ): List<CnsPoint> {
        val days = ChronoUnit.DAYS.between(from, to).toInt()

        return (0..days).mapNotNull { offset ->
            val date = from.plusDays(offset.toLong())
            val sessionOnDay = sessions
                .filter { session ->
                    Instant.ofEpochMilli(session.date)
                        .atZone(ZoneId.systemDefault()).toLocalDate() == date
                }
                .maxByOrNull { it.date }

            sessionOnDay?.let { CnsPoint(date, it.cnsScoreAtEnd) }
        }
    }

    // ─────────────────────────────────────────────
    // TRIMP CHART
    // ─────────────────────────────────────────────

    @Composable
    private fun TrImpChart(data: List<DayPoint>) {
        val totalActual   = data.sumOf { it.actual }
        val totalPlanned  = data.sumOf { it.planned }
        val pct           = if (totalPlanned > 0)
            ((totalActual / totalPlanned - 1) * 100).toInt() else 0
        val pctColor      = if (pct >= -10) AccentGreen else AccentRed
        val pctText       = if (pct >= 0) "+$pct%" else "$pct%"

        ChartCard(
            title    = "Training Load (TRIMP)",
            subtitle = "Planned vs actual · 30 days"
        ) {
            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                LegendItem(color = Color.White.copy(alpha = 0.7f), label = "Actual")
                LegendItem(color = AccentIndigo, label = "Planned", dashed = true)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                drawTrImpLines(data)
            }

            // Chips
            Row(
                modifier              = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val avgActual  = if (data.isNotEmpty()) totalActual / (data.size / 7.0) else 0.0
                val avgPlanned = if (data.isNotEmpty()) totalPlanned / (data.size / 7.0) else 0.0
                StatChip("Avg: ${"%.0f".format(avgActual)} TRIMP/wk", AccentIndigo)
                StatChip("Target: ${"%.0f".format(avgPlanned)}/wk", AccentIndigo)
                StatChip(pctText, pctColor)
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrImpLines(
        data: List<DayPoint>
    ) {
        val chartLeft   = 44.dp.toPx()
        val chartRight  = size.width - 8.dp.toPx()
        val chartTop    = 8.dp.toPx()
        val chartBottom = size.height - 20.dp.toPx()
        val chartW      = chartRight - chartLeft
        val chartH      = chartBottom - chartTop

        val maxVal = (data.maxOf { maxOf(it.actual, it.planned) } * 1.2).coerceAtLeast(10.0)

        fun valToY(v: Double): Float =
            chartBottom - ((v / maxVal) * chartH).toFloat()

        fun idxToX(i: Int): Float =
            chartLeft + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW

        // Grid
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { lvl ->
            val y = chartBottom - lvl * chartH
            drawLine(
                Color.White.copy(alpha = 0.05f),
                Offset(chartLeft, y), Offset(chartRight, y), 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${"%.0f".format(maxVal * lvl)}",
                chartLeft - 6.dp.toPx(), y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(80, 255, 255, 255)
                    textSize  = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // X labels (weeks)
        val weekIndices = listOf(0, data.size / 4, data.size / 2, 3 * data.size / 4, data.size - 1)
        weekIndices.forEach { i ->
            val x   = idxToX(i)
            val lbl = "W${i / 7 + 1}"
            drawContext.canvas.nativeCanvas.drawText(
                lbl, x, size.height,
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(60, 255, 255, 255)
                    textSize  = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Planned line (indigo dashed)
        val dashLen = 8.dp.toPx()
        val gapLen  = 5.dp.toPx()
        for (i in 0 until data.size - 1) {
            val x1 = idxToX(i);   val y1 = valToY(data[i].planned)
            val x2 = idxToX(i+1); val y2 = valToY(data[i+1].planned)
            var cursor = 0f
            val segLen = kotlin.math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1).toDouble()).toFloat()
            val dx = (x2 - x1) / segLen; val dy = (y2 - y1) / segLen
            var draw = true
            while (cursor < segLen) {
                val end = (cursor + if (draw) dashLen else gapLen).coerceAtMost(segLen)
                if (draw) {
                    drawLine(
                        AccentIndigo.copy(alpha = 0.55f),
                        Offset(x1 + dx * cursor, y1 + dy * cursor),
                        Offset(x1 + dx * end,    y1 + dy * end),
                        1.5.dp.toPx()
                    )
                }
                cursor += if (draw) dashLen else gapLen
                draw = !draw
            }
        }

        // Actual fill
        val fillPath = Path().apply {
            moveTo(idxToX(0), chartBottom)
            lineTo(idxToX(0), valToY(data[0].actual))
            for (i in 1 until data.size) lineTo(idxToX(i), valToY(data[i].actual))
            lineTo(idxToX(data.size - 1), chartBottom)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                listOf(AccentIndigo.copy(alpha = 0.3f), AccentIndigo.copy(alpha = 0.0f)),
                startY = chartTop, endY = chartBottom
            )
        )

        // Actual line (white)
        val linePath = Path().apply {
            moveTo(idxToX(0), valToY(data[0].actual))
            for (i in 1 until data.size) lineTo(idxToX(i), valToY(data[i].actual))
        }
        drawPath(linePath, Color.White.copy(alpha = 0.8f),
            style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Weekly dots
        for (i in data.indices step 7) {
            drawCircle(AccentIndigo, 3.dp.toPx(), Offset(idxToX(i), valToY(data[i].actual)))
        }
    }

    // ─────────────────────────────────────────────
    // CNS CHART
    // ─────────────────────────────────────────────

    @Composable
    private fun CnsChart(data: List<CnsPoint>) {
        val avgCns     = if (data.isNotEmpty()) data.map { it.cns }.average().toInt() else 0
        val belowCount = data.count { it.cns < 70 }
        val trend      = if (data.size >= 7) {
            val last7  = data.takeLast(7).map { it.cns }.average()
            val prev7  = data.dropLast(7).takeLast(7).map { it.cns }.average()
            when {
                last7 > prev7 + 3 -> "↑ Improving"
                last7 < prev7 - 3 -> "↓ Declining"
                else               -> "→ Stable"
            }
        } else "→ Stable"
        val trendColor = when {
            trend.startsWith("↑") -> AccentGreen
            trend.startsWith("↓") -> AccentRed
            else                  -> AccentAmber
        }

        ChartCard(
            title    = "CNS Recovery Trend",
            subtitle = "Daily readiness score · 30 days"
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                LegendItem(color = AccentGreen, label = "CNS score")
                LegendItem(color = AccentGreen.copy(alpha = 0.3f), label = "Optimal (70)", dashed = true)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                drawCnsChart(data)
            }

            Row(
                modifier              = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatChip("Avg CNS: $avgCns", AccentGreen)
                if (belowCount > 0) StatChip("$belowCount days below 70", AccentRed)
                StatChip(trend, trendColor)
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCnsChart(
        data: List<CnsPoint>
    ) {
        val chartLeft   = 44.dp.toPx()
        val chartRight  = size.width - 8.dp.toPx()
        val chartTop    = 8.dp.toPx()
        val chartBottom = size.height - 20.dp.toPx()
        val chartW      = chartRight - chartLeft
        val chartH      = chartBottom - chartTop

        fun cnsToY(cns: Int): Float =
            chartBottom - (cns / 100f) * chartH

        fun idxToX(i: Int): Float =
            chartLeft + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW

        val threshold70Y = cnsToY(70)

        // Grid + Y labels
        listOf(25, 50, 70, 100).forEach { v ->
            val y = cnsToY(v)
            drawLine(
                if (v == 70) AccentGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                Offset(chartLeft, y), Offset(chartRight, y),
                if (v == 70) 1f else 0.8f,
                pathEffect = if (v == 70) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$v",
                chartLeft - 6.dp.toPx(), y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(70, 255, 255, 255)
                    textSize  = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // Red zones below 70
        for (i in 0 until data.size - 1) {
            if (data[i].cns < 70 && data[i+1].cns < 70) {
                val x1 = idxToX(i);   val y1 = cnsToY(data[i].cns)
                val x2 = idxToX(i+1); val y2 = cnsToY(data[i+1].cns)
                val redPath = Path().apply {
                    moveTo(x1, threshold70Y)
                    lineTo(x1, y1)
                    lineTo(x2, y2)
                    lineTo(x2, threshold70Y)
                    close()
                }
                drawPath(redPath, AccentRed.copy(alpha = 0.12f))
            }
        }

        // Fill
        val fillPath = Path().apply {
            moveTo(idxToX(0), chartBottom)
            lineTo(idxToX(0), cnsToY(data[0].cns))
            for (i in 1 until data.size) lineTo(idxToX(i), cnsToY(data[i].cns))
            lineTo(idxToX(data.size - 1), chartBottom)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                listOf(AccentGreen.copy(alpha = 0.35f), AccentGreen.copy(alpha = 0.0f)),
                startY = chartTop, endY = chartBottom
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(idxToX(0), cnsToY(data[0].cns))
            for (i in 1 until data.size) lineTo(idxToX(i), cnsToY(data[i].cns))
        }
        drawPath(linePath, AccentGreen.copy(alpha = 0.9f),
            style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // ─────────────────────────────────────────────
    // ATHLETIC EVOLUTION CHART
    // ─────────────────────────────────────────────

    @Composable
    private fun AthleticEvolutionChart(snapshots: List<ScoreSnapshot>) {
        val axes = listOf(
            Triple("Strength",    AccentIndigo) { s: ScoreSnapshot -> s.strength },
            Triple("Speed",       AccentAmber)  { s: ScoreSnapshot -> s.speed },
            Triple("Endurance",   AccentGreen)  { s: ScoreSnapshot -> s.endurance },
            Triple("HRR",         AccentBlue)   { s: ScoreSnapshot -> s.hrr },
            Triple("Load Bal.",   AccentRed)    { s: ScoreSnapshot -> s.loadBalance }
        )

        // Calculăm deltas
        val chips = if (snapshots.size >= 2) {
            val first = snapshots.first()
            val last  = snapshots.last()
            axes.mapNotNull { (label, color, getter) ->
                val delta = getter(last) - getter(first)
                if (kotlin.math.abs(delta) >= 2f) {
                    Triple(label, color, delta)
                } else null
            }.sortedByDescending { kotlin.math.abs(it.third) }.take(3)
        } else emptyList()

        ChartCard(
            title    = "Athletic Profile Evolution",
            subtitle = "Score per quality · last 30 days"
        ) {
            // Legend
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                axes.forEach { (label, color, _) ->
                    LegendItem(color = color, label = label)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (snapshots.isEmpty()) return@Canvas
                drawAthleticLines(snapshots, axes)
            }

            // Chips
            if (chips.isNotEmpty()) {
                Row(
                    modifier              = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chips.forEach { (label, color, delta) ->
                        val sign = if (delta >= 0) "↑" else "↓"
                        StatChip("$sign $label ${"%.0f".format(kotlin.math.abs(delta))}pts", color)
                    }
                }
            } else if (snapshots.isEmpty()) {
                Text(
                    "Complete sessions to see evolution",
                    color    = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAthleticLines(
        snapshots: List<ScoreSnapshot>,
        axes: List<Triple<String, Color, (ScoreSnapshot) -> Float>>
    ) {
        val chartLeft   = 44.dp.toPx()
        val chartRight  = size.width - 8.dp.toPx()
        val chartTop    = 8.dp.toPx()
        val chartBottom = size.height - 20.dp.toPx()
        val chartW      = chartRight - chartLeft
        val chartH      = chartBottom - chartTop

        fun scoreToY(s: Float): Float =
            chartBottom - (s / 100f) * chartH

        fun idxToX(i: Int): Float =
            chartLeft + (i.toFloat() / (snapshots.size - 1).coerceAtLeast(1)) * chartW

        // Grid
        listOf(25f, 50f, 75f, 100f).forEach { v ->
            val y = scoreToY(v)
            drawLine(Color.White.copy(alpha = 0.05f), Offset(chartLeft, y), Offset(chartRight, y), 0.8f)
            drawContext.canvas.nativeCanvas.drawText(
                "${v.toInt()}",
                chartLeft - 6.dp.toPx(), y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(60, 255, 255, 255)
                    textSize  = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // X labels
        val step = (snapshots.size / 4).coerceAtLeast(1)
        for (i in snapshots.indices step step) {
            drawContext.canvas.nativeCanvas.drawText(
                "W${i / 7 + 1}", idxToX(i), size.height,
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(50, 255, 255, 255)
                    textSize  = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Fiecare axă
        axes.forEachIndexed { axisIdx, (_, color, getter) ->
            val path = Path()
            snapshots.forEachIndexed { i, snap ->
                val x = idxToX(i)
                val y = scoreToY(getter(snap))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Load Balance e punctat
            val isLoadBalance = axisIdx == 4
            drawPath(
                path, color.copy(alpha = 0.8f),
                style = Stroke(
                    1.5.dp.toPx(),
                    cap        = StrokeCap.Round,
                    join       = StrokeJoin.Round,
                    pathEffect = if (isLoadBalance)
                        PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null
                )
            )

            // Dot + valoare la final
            val lastSnap = snapshots.last()
            val lastX    = idxToX(snapshots.size - 1)
            val lastY    = scoreToY(getter(lastSnap))
            drawCircle(color, 3.dp.toPx(), Offset(lastX, lastY))
        }
    }

    // ─────────────────────────────────────────────
    // COMPONENTE COMUNE
    // ─────────────────────────────────────────────

    @Composable
    private fun ChartCard(
        title: String,
        subtitle: String,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp))
            content()
        }
    }

    @Composable
    private fun LegendItem(color: Color, label: String, dashed: Boolean = false) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 2.dp)
                    .background(color)
            )
            Text(label, color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
        }
    }

    @Composable
    private fun StatChip(text: String, color: Color) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }