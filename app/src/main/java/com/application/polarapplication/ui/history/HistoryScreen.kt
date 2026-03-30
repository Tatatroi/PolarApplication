package com.application.polarapplication.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import java.util.Calendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.planning.BompaCalendarHelper

// ─────────────────────────────────────────────
// CULORI TEMĂ
// ─────────────────────────────────────────────
val AppBackground  = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)

fun getThemeForWorkout(type: String): WorkoutTheme {
    return when (type.uppercase()) {
        "STRENGTH" -> WorkoutTheme(Color(0xFFFF3B30), "High Load")
        "ENDURANCE" -> WorkoutTheme(Color(0xFF34C759), "Aerobic Base")
        "SPEED"    -> WorkoutTheme(Color(0xFFFF9500), "Anaerobic Load")
        "RECOVERY" -> WorkoutTheme(Color(0xFF007AFF), "Active Recovery")
        else       -> WorkoutTheme(Color(0xFF00E5FF), "General Load")
    }
}

data class WorkoutTheme(val color: Color, val label: String)

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel(),
    onSessionClick: (TrainingSessionEntity) -> Unit
) {
    val sessions        by viewModel.allSessions.collectAsState()
    val competitionDate by viewModel.competitionDate.collectAsState()
    val planStartDate   by viewModel.planStartDate.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Istoric Antrenamente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )

        // ── Countdown competiție ──────────────────────────────────────────────
        competitionDate?.let { compDate ->
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), compDate)
            if (daysLeft >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A0808))
                        .border(
                            1.dp,
                            Color(0xFFEF4444).copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMPETIȚIE TARGET",
                            color = Color(0xFFF87171).copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = compDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$daysLeft",
                            color = Color(0xFFF87171),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = "zile rămase",
                            color = Color(0xFFF87171).copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ── Tab-uri ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CardSurfaceDark, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("Vizualizare Listă", "Calendar").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedTabIndex == index) Color(0xFF2A2A35)
                            else Color.Transparent
                        )
                        .clickable { selectedTabIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedTabIndex == index) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty() && selectedTabIndex == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nicio sesiune înregistrată.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            if (selectedTabIndex == 0) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        PremiumHistoryCard(
                            session  = session,
                            onClick  = { onSessionClick(session) },
                            onDelete = { viewModel.deleteSession(session) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            } else {
                WorkoutCalendar(
                    sessions        = sessions,
                    onSessionClick  = onSessionClick,
                    planStartDate   = planStartDate,
                    competitionDate = competitionDate
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CARD ISTORUC
// ─────────────────────────────────────────────

@Composable
fun PremiumHistoryCard(
    session: TrainingSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(session.date))
    val theme   = getThemeForWorkout(session.type)

    val cardBackgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            theme.color.copy(alpha = 0.15f),
            CardSurfaceDark,
            theme.color.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(theme.color.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        IconButton(
            onClick  = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .offset(x = 8.dp, y = (-8).dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Șterge", tint = Color.Gray.copy(alpha = 0.5f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text     = session.type.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color    = Color.White,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(end = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = dateStr, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avg HR", color = Color.Gray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${session.avgHeartRate}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(" bpm", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TRIMP Score", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text     = "%.1f".format(session.finalTrimp),
                            color    = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                IntensitySegmentedBar(color = theme.color, trimp = session.finalTrimp.toFloat())
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(theme.color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = theme.label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            CnsCircularRing(cnsValue = session.cnsScoreAtEnd, color = theme.color)
        }
    }
}

@Composable
fun CnsCircularRing(cnsValue: Int, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(start = 8.dp).size(85.dp).aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            drawArc(color = Color.DarkGray.copy(alpha = 0.2f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            val progressAngle = (cnsValue / 100f) * 360f
            drawArc(brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.4f), color)), startAngle = -90f, sweepAngle = progressAngle, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$cnsValue%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "CNS Final", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun IntensitySegmentedBar(color: Color, trimp: Float) {
    val percentage = (trimp / 150f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)) {
        val segmentWidth  = 3.dp.toPx()
        val segmentSpacing = 3.dp.toPx()
        val totalSegments  = (size.width / (segmentWidth + segmentSpacing)).toInt()
        val activeSegments = (totalSegments * percentage).toInt()
        for (i in 0 until totalSegments) {
            val isActive = i < activeSegments
            val alpha    = if (isActive) 1f - (i.toFloat() / totalSegments * 0.5f) else 0.1f
            drawRect(color = color.copy(alpha = alpha), topLeft = Offset(i * (segmentWidth + segmentSpacing), 0f), size = Size(segmentWidth, size.height))
        }
    }
}

// ─────────────────────────────────────────────
// CALENDAR
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCalendar(
    sessions: List<TrainingSessionEntity>,
    onSessionClick: (TrainingSessionEntity) -> Unit,
    planStartDate: LocalDate?,
    competitionDate: LocalDate?
) {
    var currentMonthOffset by remember { mutableStateOf(0) }
    var sessionsToSelect by remember { mutableStateOf<List<TrainingSessionEntity>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayCalendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, currentMonthOffset) }
    }

    val currentViewMonth = displayCalendar.get(Calendar.MONTH)
    val currentViewYear  = displayCalendar.get(Calendar.YEAR)

    val monthSetupCalendar = displayCalendar.clone() as Calendar
    monthSetupCalendar.set(Calendar.DAY_OF_MONTH, 1)

    var firstDayOfWeek = monthSetupCalendar.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    val daysInMonth = monthSetupCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val sessionsByDay: Map<Int, List<TrainingSessionEntity>> = sessions.filter { session ->
        val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
        sessionCal.get(Calendar.YEAR) == currentViewYear &&
                sessionCal.get(Calendar.MONTH) == currentViewMonth
    }.groupBy { session ->
        Calendar.getInstance().apply { timeInMillis = session.date }.get(Calendar.DAY_OF_MONTH)
    }

    val realTodayCalendar      = Calendar.getInstance()
    val isViewingCurrentMonthAndYear =
        realTodayCalendar.get(Calendar.YEAR)  == currentViewYear &&
                realTodayCalendar.get(Calendar.MONTH) == currentViewMonth
    val realTodayDayOfMonth    = realTodayCalendar.get(Calendar.DAY_OF_MONTH)
    val weekDays               = listOf("L", "M", "M", "J", "V", "S", "D")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurfaceDark)
            .padding(16.dp)
    ) {
        // Header navigare luni
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = { currentMonthOffset -= 1 },
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Înapoi", tint = Color.White) }

            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayCalendar.time).uppercase()
            Text(text = monthName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            IconButton(
                onClick  = { currentMonthOffset += 1 },
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Înainte", tint = Color.White) }
        }

        // Legendă Bompa (doar dacă există plan)
        if (planStartDate != null && competitionDate != null) {
            BompaLegend()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Zilele săptămânii
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { day ->
                Text(text = day, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows       = Math.ceil(totalCells / 7.0).toInt()

        LazyVerticalGrid(
            columns               = GridCells.Fixed(7),
            modifier              = Modifier.height((rows * 58).dp),
            userScrollEnabled     = false,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement   = Arrangement.spacedBy(4.dp)
        ) {
            items(totalCells) { index ->
                val day = index - firstDayOfWeek + 1
                if (day > 0 && day <= daysInMonth) {
                    val cellDate    = LocalDate.of(currentViewYear, currentViewMonth + 1, day)
                    val dailySessions = sessionsByDay[day] ?: emptyList()
                    val isToday     = isViewingCurrentMonthAndYear && day == realTodayDayOfMonth
                    val isCompDay   = competitionDate != null && cellDate == competitionDate

                    // Workout planificat pentru această zi
                    val plannedWorkout = if (planStartDate != null && competitionDate != null) {
                        BompaCalendarHelper.getPlannedWorkout(cellDate, planStartDate, competitionDate)
                    } else null

                    CalendarDayCell(
                        day             = day,
                        dailySessions   = dailySessions,
                        isToday         = isToday,
                        plannedWorkout  = plannedWorkout,
                        isCompetitionDay = isCompDay,
                        onClick         = {
                            if (dailySessions.size == 1) {
                                onSessionClick(dailySessions.first())
                            } else if (dailySessions.size > 1) {
                                sessionsToSelect = dailySessions
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }

    // Bottom sheet pentru zile cu mai multe sesiuni
    if (sessionsToSelect != null) {
        ModalBottomSheet(
            onDismissRequest = { sessionsToSelect = null },
            sheetState       = sheetState,
            containerColor   = CardSurfaceDark,
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth()) {
                Text(text = "Alege sesiunea", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                sessionsToSelect!!.forEach { session ->
                    val theme   = getThemeForWorkout(session.type)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.date))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, theme.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { sessionsToSelect = null; onSessionClick(session) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(session.type.uppercase(), color = theme.color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Ora: $timeStr • TRIMP: ${"%.1f".format(session.finalTrimp)}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// LEGENDĂ BOMPA
// ─────────────────────────────────────────────

@Composable
private fun BompaLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicator plan normal
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF818CF8).copy(alpha = 0.2f))
                    .border(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("F", color = Color(0xFF818CF8), fontSize = 6.sp, fontWeight = FontWeight.Black)
            }
            Text("Plan Bompa", color = Color(0xFF555566), fontSize = 9.sp)
        }

        // Indicator warning
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFF97316).copy(alpha = 0.2f))
                    .border(0.5.dp, Color(0xFFF97316).copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("!", color = Color(0xFFF97316), fontSize = 6.sp, fontWeight = FontWeight.Black)
            }
            Text("Deviere plan", color = Color(0xFF555566), fontSize = 9.sp)
        }

        // Marker competiție
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFEF4444)))
            Text("Competiție", color = Color(0xFF555566), fontSize = 9.sp)
        }
    }
}

// ─────────────────────────────────────────────
// CELULA ZI DIN CALENDAR
// ─────────────────────────────────────────────

@Composable
fun CalendarDayCell(
    day: Int,
    dailySessions: List<TrainingSessionEntity>,
    isToday: Boolean,
    onClick: () -> Unit,
    plannedWorkout: WorkoutType?    = null,
    isCompetitionDay: Boolean       = false
) {
    val sessionCount  = dailySessions.size
    val hasWorkout    = sessionCount > 0
    val sessionColors = dailySessions.map { getThemeForWorkout(it.type).color }

    // Warning dacă prima sesiune diferă de tipul planificat
    val hasWarning = hasWorkout && BompaCalendarHelper.isOffPlan(
        dailySessions.first().type,
        plannedWorkout
    )

    // Fundal: dacă avem plan → culoarea fazei Bompa, altfel default
    val cellBackground: Color = when {
        isCompetitionDay -> Color(0xFF1A0808)
        hasWorkout && sessionColors.size == 1 -> sessionColors.first().copy(alpha = 0.15f)
        hasWorkout -> Color(0xFF15151C)
        else -> Color(0xFF1E1E28)
    }

    val borderBrush = when {
        isCompetitionDay -> SolidColor(Color(0xFFEF4444).copy(alpha = 0.6f))
        hasWorkout && sessionColors.size == 1 -> SolidColor(sessionColors.first().copy(alpha = 0.5f))
        hasWorkout -> Brush.linearGradient(sessionColors.map { it.copy(alpha = 0.7f) })
        isToday    -> SolidColor(Color.White.copy(alpha = 0.4f))
        else       -> SolidColor(Color.Transparent)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(cellBackground)
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(10.dp))
            .clickable(enabled = hasWorkout || isCompetitionDay) { onClick() },
        contentAlignment = Alignment.Center
    ) {

        // ── Conținut central ──────────────────────────────────────────────────
        when {
            isCompetitionDay -> Icon(Icons.Default.EmojiEvents, contentDescription = "Competiție", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
            hasWorkout && sessionCount == 1 -> Icon(
                imageVector     = Icons.Default.Check,
                contentDescription = null,
                tint            = sessionColors.first(),
                modifier        = Modifier.size(20.dp)
            )
            hasWorkout -> Text(
                text       = "+$sessionCount",
                color      = Color.White,
                fontWeight = FontWeight.Black,
                fontSize   = 14.sp
            )
        }

        // ── Numărul zilei (sus-dreapta) ───────────────────────────────────────
        Text(
            text       = "$day",
            color      = when {
                isToday    -> Color.White
                hasWorkout -> Color.White.copy(alpha = 0.7f)
                else       -> Color(0xFF555566)
            },
            fontSize   = if (isToday) 12.sp else 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 3.dp)
        )

        // ── Indicator Bompa (sus-stânga) ──────────────────────────────────────
        // Vizibil pe toate zilele din intervalul planului, nu doar pe cele cu sesiuni
        // ── Indicator Bompa (sus-stânga) ──────────────────────────────────────
        // ── Indicator Bompa (sus-stânga) ──────────────────────────────────────
        plannedWorkout?.let { planned ->
            if (planned != WorkoutType.REST) {
                val indicatorColor = when {
                    hasWarning -> Color(0xFFF97316)
                    else       -> BompaCalendarHelper.workoutColor(planned)
                }
                val letter = when {
                    hasWarning -> "!"
                    else       -> BompaCalendarHelper.workoutLetter(planned)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        // fără padding — marginile se suprapun cu colțul celulei
                        .size(18.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 6.dp))
                        .background(indicatorColor.copy(alpha = 0.25f))
                        .border(
                            width = 0.5.dp,
                            color = indicatorColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = letter,
                        color      = indicatorColor,
                        fontSize   = 9.sp,          // mai mare și vizibil
                        fontWeight = FontWeight.Black,
                        lineHeight = 9.sp           // previne offset vertical
                    )
                }
            }
        }
    }
}