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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import java.util.Calendar

// --- Paleta de Culori Exacte din Design ---
val AppBackground = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)

// Helper pentru a extrage tema în funcție de antrenament
fun getThemeForWorkout(type: String): WorkoutTheme {
    return when (type.uppercase()) {
        "STRENGTH" -> WorkoutTheme(Color(0xFFFF3B30), "High Load")
        "ENDURANCE" -> WorkoutTheme(Color(0xFF34C759), "Aerobic Base")
        "SPEED" -> WorkoutTheme(Color(0xFFFF9500), "Anaerobic Load")
        "RECOVERY" -> WorkoutTheme(Color(0xFF007AFF), "Active Recovery")
        else -> WorkoutTheme(Color(0xFF00E5FF), "General Load")
    }
}

data class WorkoutTheme(val color: Color, val label: String)

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel(),
    onSessionClick: (TrainingSessionEntity) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState()

    // STATE: 0 = Listă, 1 = Calendar
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
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        // --- CUSTOM SEGMENTED CONTROL (TABS) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CardSurfaceDark, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            // Tab 1: Lista
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTabIndex == 0) Color(0xFF2A2A35) else Color.Transparent)
                    .clickable { selectedTabIndex = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vizualizare Listă",
                    color = if (selectedTabIndex == 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Tab 2: Calendar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTabIndex == 1) Color(0xFF2A2A35) else Color.Transparent)
                    .clickable { selectedTabIndex = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Calendar",
                    color = if (selectedTabIndex == 1) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nicio sesiune înregistrată.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            if (selectedTabIndex == 0) {
                // Afișăm doar Lista
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        PremiumHistoryCard(
                            session = session,
                            onClick = { onSessionClick(session) },
                            onDelete = { viewModel.deleteSession(session) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            } else {
                WorkoutCalendar(
                    sessions = sessions,
                    onSessionClick = onSessionClick
                )
            }
        }
    }
}

@Composable
fun PremiumHistoryCard(
    session: TrainingSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(session.date))
    val theme = getThemeForWorkout(session.type)

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
        // Iconița de ștergere (Rămâne sus în dreapta)
        IconButton(
            onClick = onDelete,
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
            // --- PARTEA STÂNGĂ (Detalii) ---
            Column(modifier = Modifier.weight(1f)) {

                // === MODIFICAREA ESTE AICI ===
                // Header (Titlu și Dată puse unul sub altul, nu unul lângă altul)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = session.type.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        // Adăugăm padding în dreapta ca titlul lung să nu intre peste iconița de delete
                        modifier = Modifier.padding(end = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                // ==============================

                Spacer(modifier = Modifier.height(24.dp))

                // Metrici Principale
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Avg HR
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avg HR", color = Color.Gray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${session.avgHeartRate}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(" bpm", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                        }
                    }
                    // TRIMP
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TRIMP Score", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "%.1f".format(session.finalTrimp),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bara segmentată de intensitate
                IntensitySegmentedBar(color = theme.color, trimp = session.finalTrimp.toFloat())

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(theme.color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = theme.label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // --- PARTEA DREAPTĂ (CNS Ring) ---
            CnsCircularRing(cnsValue = session.cnsScoreAtEnd, color = theme.color)
        }
    }
}

// Custom Composable pentru inelul luminos CNS
@Composable
fun CnsCircularRing(cnsValue: Int, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        // MODIFICAREA E AICI: Mai întâi padding, apoi mărime fixă + aspectRatio
        modifier = Modifier
            .padding(start = 8.dp)
            .size(85.dp)
            .aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()

            // Inelul de fundal
            drawArc(
                color = Color.DarkGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Inelul colorat luminos
            val progressAngle = (cnsValue / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.4f), color)),
                startAngle = -90f,
                sweepAngle = progressAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Textul central
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$cnsValue%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "CNS Final",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
// Custom Composable pentru bara segmentată
@Composable
fun IntensitySegmentedBar(color: Color, trimp: Float) {
    val percentage = (trimp / 150f).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)) {
        val segmentWidth = 3.dp.toPx()
        val segmentSpacing = 3.dp.toPx()
        val totalSegments = (size.width / (segmentWidth + segmentSpacing)).toInt()
        val activeSegments = (totalSegments * percentage).toInt()

        for (i in 0 until totalSegments) {
            val isActive = i < activeSegments
            val alpha = if (isActive) 1f - (i.toFloat() / totalSegments * 0.5f) else 0.1f

            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(i * (segmentWidth + segmentSpacing), 0f),
                size = Size(segmentWidth, size.height)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCalendar(
    sessions: List<TrainingSessionEntity>,
    onSessionClick: (TrainingSessionEntity) -> Unit
) {
    var currentMonthOffset by remember { mutableStateOf(0) }

    // STATE PENTRU MENIUL DE JOS (Bottom Sheet)
    var sessionsToSelect by remember { mutableStateOf<List<TrainingSessionEntity>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayCalendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, currentMonthOffset) }
    }

    val currentViewMonth = displayCalendar.get(Calendar.MONTH)
    val currentViewYear = displayCalendar.get(Calendar.YEAR)

    val monthSetupCalendar = displayCalendar.clone() as Calendar
    monthSetupCalendar.set(Calendar.DAY_OF_MONTH, 1)

    var firstDayOfWeek = monthSetupCalendar.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    val daysInMonth = monthSetupCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val sessionsByDay: Map<Int, List<TrainingSessionEntity>> = sessions.filter { session ->
        val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
        sessionCal.get(Calendar.YEAR) == currentViewYear && sessionCal.get(Calendar.MONTH) == currentViewMonth
    }.groupBy { session ->
        Calendar.getInstance().apply { timeInMillis = session.date }.get(Calendar.DAY_OF_MONTH)
    }

    val realTodayCalendar = Calendar.getInstance()
    val isViewingCurrentMonthAndYear =
        realTodayCalendar.get(Calendar.YEAR) == currentViewYear &&
                realTodayCalendar.get(Calendar.MONTH) == currentViewMonth
    val realTodayDayOfMonth = realTodayCalendar.get(Calendar.DAY_OF_MONTH)

    val weekDays = listOf("L", "M", "M", "J", "V", "S", "D")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurfaceDark)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { currentMonthOffset -= 1 },
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Înapoi", tint = Color.White) }

            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayCalendar.time).uppercase()
            Text(text = monthName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            IconButton(
                onClick = { currentMonthOffset += 1 },
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Înainte", tint = Color.White) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { day -> Text(text = day, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = Math.ceil(totalCells / 7.0).toInt()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height((rows * 50).dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(totalCells) { index ->
                val day = index - firstDayOfWeek + 1

                if (day > 0 && day <= daysInMonth) {
                    val dailySessions = sessionsByDay[day] ?: emptyList()
                    val isToday = isViewingCurrentMonthAndYear && day == realTodayDayOfMonth

                    CalendarDayCell(
                        day = day,
                        dailySessions = dailySessions,
                        isToday = isToday,
                        onClick = {
                            if (dailySessions.size == 1) {
                                // Dacă e doar unul, mergem direct la detalii
                                onSessionClick(dailySessions.first())
                            } else if (dailySessions.size > 1) {
                                // Dacă sunt mai multe, deschidem Bottom Sheet-ul
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

    if (sessionsToSelect != null) {
        ModalBottomSheet(
            onDismissRequest = { sessionsToSelect = null },
            sheetState = sheetState,
            containerColor = CardSurfaceDark,
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth()) {
                Text(
                    text = "Alege sesiunea",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                sessionsToSelect!!.forEach { session ->
                    val theme = getThemeForWorkout(session.type)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.date))

                    // Card mic pentru fiecare opțiune din meniu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, theme.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                sessionsToSelect = null
                                onSessionClick(session)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(session.type.uppercase(), color = theme.color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Ora: $timeStr • TRIMP: ${"%.1f".format(session.finalTrimp)}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Vezi detaliile", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    dailySessions: List<TrainingSessionEntity>,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val sessionCount = dailySessions.size
    val hasWorkout = sessionCount > 0

    val sessionColors = dailySessions.map { getThemeForWorkout(it.type).color }

    val bgBrush = when {
        !hasWorkout -> SolidColor(Color(0xFF1E1E28)) // Gri gol
        sessionColors.size == 1 -> SolidColor(sessionColors.first().copy(alpha = 0.15f))
        else -> Brush.linearGradient(sessionColors.map { it.copy(alpha = 0.2f) })
    }

    val borderBrush = when {
        !hasWorkout -> SolidColor(if (isToday) Color.White.copy(alpha = 0.4f) else Color.Transparent)
        sessionColors.size == 1 -> SolidColor(sessionColors.first().copy(alpha = 0.5f))
        else -> Brush.linearGradient(sessionColors.map { it.copy(alpha = 0.7f) })
    }

    val textColor = if (hasWorkout) Color.White else Color.Gray

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = hasWorkout) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasWorkout) {
            if (sessionCount == 1) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completat",
                    tint = sessionColors.first(),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "+$sessionCount",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "$day",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp)
            )
        } else {
            Text(
                text = "$day",
                color = if (isToday) Color.White else textColor,
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}