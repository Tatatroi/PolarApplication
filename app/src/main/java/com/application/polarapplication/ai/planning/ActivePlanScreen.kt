package com.application.polarapplication.ui.planning

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.planning.MesoCycle
import com.application.polarapplication.ai.planning.MicroCycle
import com.application.polarapplication.ai.planning.TrainingPlanner
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgDark       = Color(0xFF080808)
private val GlassBg      = Color(0x0AFFFFFF)
private val GlassBorder  = Color(0x14FFFFFF)
private val GlassSmBg    = Color(0x0DFFFFFF)
private val GlassSmBorder= Color(0x17FFFFFF)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentRed    = Color(0xFFF87171)
private val AccentAmber  = Color(0xFFFBBF24)
private val AccentBlue   = Color(0xFF60A5FA)
private val AccentCyan   = Color(0xFF67E8F9)
private val AccentPurple = Color(0xFFA78BFA)

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────
private fun phaseColor(phase: String) = when (phase.lowercase()) {
    "general"  -> AccentIndigo
    "specific" -> AccentAmber
    "precomp"  -> AccentPurple
    "comp"     -> AccentRed
    "recovery" -> AccentCyan
    else       -> Color.Gray
}

private fun phaseBg(phase: String) = when (phase.lowercase()) {
    "general"  -> Color(0xFF1A1A3E)
    "specific" -> Color(0xFF2A1F00)
    "precomp"  -> Color(0xFF1A0D2E)
    "comp"     -> Color(0xFF1A0808)
    "recovery" -> Color(0xFF0D1A1A)
    else       -> GlassBg
}

private fun blockName(phase: String) = when (phase.lowercase()) {
    "general"  -> "Base Strength Block"
    "specific" -> "Sport-Specific Block"
    "precomp"  -> "Pre-Competition Block"
    "comp"     -> "Competition Block"
    "recovery" -> "Recovery Block"
    else       -> phase
}

private fun blockDesc(phase: String) = when (phase.lowercase()) {
    "general"  -> "Build your aerobic foundation and general strength. High volume, moderate intensity. This phase sets the base for everything that follows."
    "specific" -> "Transfer general fitness to sport demands. Increased intensity, moderate volume. Focus on explosive power and sport-specific movements."
    "precomp"  -> "Competition simulation. Low volume, maximum intensity. Peak performance preparation and fine-tuning."
    "comp"     -> "Maintain peak form. Short, explosive sessions. Taper volume while keeping intensity high."
    "recovery" -> "Full neuromuscular recovery. Light activity, mobility work, sleep priority."
    else       -> ""
}

private fun workoutColor(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> AccentIndigo
    WorkoutType.ENDURANCE -> AccentGreen
    WorkoutType.SPEED     -> AccentAmber
    WorkoutType.RECOVERY  -> AccentBlue
    WorkoutType.REST      -> Color(0xFF444455)
}

private fun workoutBg(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> Color(0xFF1A1A3E)
    WorkoutType.ENDURANCE -> Color(0xFF0F2A1A)
    WorkoutType.SPEED     -> Color(0xFF2A1F00)
    WorkoutType.RECOVERY  -> Color(0xFF0A1A2A)
    WorkoutType.REST      -> Color(0xFF111118)
}

private fun workoutLabel(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "STR"
    WorkoutType.ENDURANCE -> "END"
    WorkoutType.SPEED     -> "SPD"
    WorkoutType.RECOVERY  -> "REC"
    WorkoutType.REST      -> "REST"
}

private fun workoutName(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "Maximum Strength"
    WorkoutType.ENDURANCE -> "Aerobic Endurance"
    WorkoutType.SPEED     -> "Speed & Explosiveness"
    WorkoutType.RECOVERY  -> "Active Recovery"
    WorkoutType.REST      -> "Full Rest Day"
}

private fun workoutDesc(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "Heavy compound lifts. Focus on progressive overload."
    WorkoutType.ENDURANCE -> "Steady-state cardio at 65–75% HRmax. Build your aerobic engine."
    WorkoutType.SPEED     -> "Short sprints and plyometrics. Maximum explosive output."
    WorkoutType.RECOVERY  -> "Light movement, mobility work. Let your body repair."
    WorkoutType.REST      -> "Complete rest. Sleep and nutrition are your training today."
}

private fun workoutDuration(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "45–60 min"
    WorkoutType.ENDURANCE -> "30–45 min"
    WorkoutType.SPEED     -> "30–40 min"
    WorkoutType.RECOVERY  -> "20–30 min"
    WorkoutType.REST      -> "—"
}

private fun workoutIntensity(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "80–85% 1RM · 5×5 sets"
    WorkoutType.ENDURANCE -> "65–75% HRmax"
    WorkoutType.SPEED     -> "10×20 sec max sprint"
    WorkoutType.RECOVERY  -> "Below 65% HRmax"
    WorkoutType.REST      -> "Rest"
}

private fun workoutIcon(type: WorkoutType): ImageVector = when (type) {
    WorkoutType.STRENGTH  -> Icons.Default.FitnessCenter
    WorkoutType.ENDURANCE -> Icons.Default.DirectionsRun
    WorkoutType.SPEED     -> Icons.Default.Speed
    WorkoutType.RECOVERY  -> Icons.Default.SelfImprovement
    WorkoutType.REST      -> Icons.Default.Hotel
}

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@Composable
fun ActivePlanScreen(
    viewModel: DashboardViewModel = viewModel(),
    onGenerateNewPlan: () -> Unit
) {
    val competitionDate by viewModel.competitionDate.collectAsState()
    val planStartDate   by viewModel.planStartDate.collectAsState()
    val today           = remember { LocalDate.now() }

    if (competitionDate == null || planStartDate == null) {
        NoPlanScreen(onGenerateNewPlan = onGenerateNewPlan)
        return
    }

    val effectiveStart = planStartDate!!
    val effectiveComp  = competitionDate!!

    val planner = remember { TrainingPlanner() }
    val plan    = remember(effectiveStart, effectiveComp) {
        planner.generatePlan(effectiveComp, effectiveStart)
    }

    val totalWeeks  = plan.mesoCycles.sumOf { it.microCycle.size }
    val totalDays   = ChronoUnit.DAYS.between(effectiveStart, effectiveComp).toInt().coerceAtLeast(1)
    val elapsedDays = ChronoUnit.DAYS.between(effectiveStart, today).toInt().coerceIn(0, totalDays)
    val progressFraction = elapsedDays.toFloat() / totalDays.toFloat()
    val currentWeekNum   = ((elapsedDays / 7) + 1).coerceIn(1, totalWeeks)
    val daysToComp       = ChronoUnit.DAYS.between(today, effectiveComp).coerceAtLeast(0)

    val currentMeso = plan.mesoCycles.firstOrNull { meso ->
        meso.microCycle.any { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
    } ?: plan.mesoCycles.first()

    val currentMicro = currentMeso.microCycle.firstOrNull {
        !it.startDate.isAfter(today) && !it.endDate.isBefore(today)
    } ?: currentMeso.microCycle.first()

    val todayIndex        = (today.dayOfWeek.value - 1).coerceIn(0, 6)
    val todayWorkoutType  = currentMicro.workouts.getOrNull(todayIndex) ?: WorkoutType.REST
    val tomorrowType      = currentMicro.workouts.getOrNull((todayIndex + 1) % 7) ?: WorkoutType.REST

    val weeksInCurrentBlock = currentMeso.microCycle.size
    val currentWeekInBlock  = currentMeso.microCycle
        .indexOfFirst { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
        .plus(1).coerceAtLeast(1)
    val weeksLeftInBlock    = (weeksInCurrentBlock - currentWeekInBlock).coerceAtLeast(0)
    val blockProgress       = currentWeekInBlock.toFloat() / weeksInCurrentBlock.toFloat()

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        PlanHeader(
            daysToComp      = daysToComp,
            totalWeeks      = totalWeeks,
            competitionDate = effectiveComp,
            onNewPlan       = onGenerateNewPlan
        )

        // ── 3 Tabs ───────────────────────────────────────────────────────────
        TabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // ── Content ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ThisWeekTab(
                    micro           = currentMicro,
                    today           = today,
                    todayType       = todayWorkoutType,
                    tomorrowType    = tomorrowType,
                    currentWeekNum  = currentWeekNum,
                    totalWeeks      = totalWeeks,
                    phaseName       = currentMeso.phase
                )
                1 -> CurrentBlockTab(
                    meso              = currentMeso,
                    today             = today,
                    currentWeekInBlock = currentWeekInBlock,
                    weeksLeftInBlock  = weeksLeftInBlock,
                    blockProgress     = blockProgress
                )
                2 -> FullPlanTab(
                    plan              = plan,
                    today             = today,
                    progressFraction  = progressFraction,
                    currentWeekNum    = currentWeekNum,
                    totalWeeks        = totalWeeks,
                    planStart         = effectiveStart,
                    competitionDate   = effectiveComp,
                    onGenerateNewPlan = onGenerateNewPlan,
                    viewModel         = viewModel
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// NO PLAN SCREEN
// ─────────────────────────────────────────────

@Composable
private fun NoPlanScreen(onGenerateNewPlan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Default.EventNote,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.2f),
            modifier           = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text       = "No Active Plan",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign  = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Generate a Bompa periodization plan based on your competition date.",
            color     = Color.White.copy(alpha = 0.3f),
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick  = onGenerateNewPlan,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.2f)),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
        ) {
            Text(
                "Generate Training Plan",
                color      = AccentIndigo,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────

@Composable
private fun PlanHeader(
    daysToComp: Long,
    totalWeeks: Int,
    competitionDate: LocalDate,
    onNewPlan: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Column {
            Text(
                text       = "Training Plan",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text  = "Bompa Periodization · $totalWeeks weeks",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentRed.copy(alpha = 0.1f))
                    .border(1.dp, AccentRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = "$daysToComp",
                        color      = AccentRed,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 24.sp
                    )
                    Text(
                        text  = "days left",
                        color = AccentRed.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TAB ROW
// ─────────────────────────────────────────────

@Composable
private fun TabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple(Icons.Default.CalendarToday, "This Week", "Microcycle"),
        Triple(Icons.Default.LocalFireDepartment, "Current Block", "Mesocycle"),
        Triple(Icons.Default.Map, "Full Plan", "Macrocycle")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, (icon, name, sub) ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF141420) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) GlassSmBorder else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = name,
                    tint               = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text       = name,
                    color      = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = sub,
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TAB 1 — THIS WEEK
// ─────────────────────────────────────────────

@Composable
private fun ThisWeekTab(
    micro: MicroCycle,
    today: LocalDate,
    todayType: WorkoutType,
    tomorrowType: WorkoutType,
    currentWeekNum: Int,
    totalWeeks: Int,
    phaseName: String
) {
    val color = workoutColor(todayType)
    val bg    = workoutBg(todayType)
    val fmt   = DateTimeFormatter.ofPattern("EEEE")
    val todayName     = today.format(fmt).uppercase()
    val tomorrowName  = today.plusDays(1).format(fmt).uppercase()

    // TODAY card
    SectionLabel("TODAY")
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text          = todayName,
            color         = Color.White.copy(alpha = 0.3f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(workoutIcon(todayType), null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text       = workoutLabel(todayType),
                color      = color,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                modifier   = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(workoutName(todayType), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(workoutDesc(todayType), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoChip(icon = Icons.Default.Timer, text = workoutDuration(todayType), color = color)
            InfoChip(icon = Icons.Default.Bolt, text = workoutIntensity(todayType), color = color)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // TOMORROW
    SectionLabel("TOMORROW")
    Spacer(modifier = Modifier.height(6.dp))
    val tColor = workoutColor(tomorrowType)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141420))
            .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(tColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(workoutIcon(tomorrowType), null, tint = tColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(tomorrowName, color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(workoutName(tomorrowType), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(tColor.copy(alpha = 0.1f))
                .border(1.dp, tColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(workoutLabel(tomorrowType), color = tColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // WEEK OVERVIEW
    SectionLabel("WEEK $currentWeekNum OF $totalWeeks")
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111118))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val dayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            micro.workouts.forEachIndexed { index, type ->
                val date     = micro.startDate.plusDays(index.toLong())
                val isToday  = date == today
                val isPast   = date.isBefore(today)
                val wColor   = workoutColor(type)
                val wBg      = workoutBg(type)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isToday -> wColor.copy(alpha = 0.12f)
                                isPast  -> AccentGreen.copy(alpha = 0.05f)
                                else    -> Color.White.copy(alpha = 0.02f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isToday -> wColor.copy(alpha = 0.4f)
                                isPast  -> AccentGreen.copy(alpha = 0.15f)
                                else    -> Color.White.copy(alpha = 0.05f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text      = dayLabels[index],
                        color     = if (isToday) wColor else Color.White.copy(alpha = 0.2f),
                        fontSize  = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier         = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    isPast  -> AccentGreen.copy(alpha = 0.15f)
                                    isToday -> wColor.copy(alpha = 0.2f)
                                    else    -> wBg
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isPast -> Icon(Icons.Default.Check, null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                            isToday -> Icon(Icons.Default.RadioButtonChecked, null, tint = wColor, modifier = Modifier.size(12.dp))
                            else -> Icon(workoutIcon(type), null, tint = wColor.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text      = workoutLabel(type),
                        color     = when {
                            isPast  -> AccentGreen.copy(alpha = 0.6f)
                            isToday -> wColor
                            else    -> wColor.copy(alpha = 0.4f)
                        },
                        fontSize  = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TAB 2 — CURRENT BLOCK
// ─────────────────────────────────────────────

@Composable
private fun CurrentBlockTab(
    meso: MesoCycle,
    today: LocalDate,
    currentWeekInBlock: Int,
    weeksLeftInBlock: Int,
    blockProgress: Float
) {
    val color   = phaseColor(meso.phase)
    val bg      = phaseBg(meso.phase)
    val fmt     = DateTimeFormatter.ofPattern("MMM dd")
    val animatedProgress by animateFloatAsState(
        targetValue   = blockProgress,
        animationSpec = tween(1000),
        label         = "blockProgress"
    )

    // Block header
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text          = "CURRENT TRAINING BLOCK",
            color         = Color.White.copy(alpha = 0.3f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(blockName(meso.phase), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text(blockDesc(meso.phase), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, lineHeight = 17.sp)

        Spacer(modifier = Modifier.height(14.dp))

        // Stats grid
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Week", "$currentWeekInBlock of ${meso.microCycle.size}", "in block"),
                Triple("Remaining", "$weeksLeftInBlock", "weeks left"),
                Triple("Ends", meso.endDate.format(fmt), ""),
            ).forEach { (label, value, sub) ->
                Column(
                    modifier            = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 20.sp)
                    if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.2f), fontSize = 9.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Progress bar
    SectionLabel("BLOCK PROGRESS")
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111118))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(blockName(meso.phase), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "$currentWeekInBlock / ${meso.microCycle.size} weeks",
                color      = color,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Weekly breakdown
    SectionLabel("WEEKLY BREAKDOWN")
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111118))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        meso.microCycle.forEachIndexed { index, micro ->
            val weekNum     = index + 1
            val weekIsPast  = micro.endDate.isBefore(today)
            val weekIsCurrent = !micro.startDate.isAfter(today) && !micro.endDate.isBefore(today)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            weekIsCurrent -> color.copy(alpha = 0.1f)
                            weekIsPast    -> AccentGreen.copy(alpha = 0.04f)
                            else          -> Color.White.copy(alpha = 0.02f)
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            weekIsCurrent -> color.copy(alpha = 0.3f)
                            weekIsPast    -> AccentGreen.copy(alpha = 0.1f)
                            else          -> Color.White.copy(alpha = 0.04f)
                        },
                        RoundedCornerShape(9.dp)
                    )
                    .padding(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text      = "W$weekNum",
                    color     = when {
                        weekIsCurrent -> color
                        weekIsPast    -> AccentGreen.copy(alpha = 0.6f)
                        else          -> Color.White.copy(alpha = 0.2f)
                    },
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier  = Modifier.width(24.dp)
                )

                // Day dots
                Row(
                    modifier              = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    micro.workouts.forEach { type ->
                        val wColor = workoutColor(type)
                        Box(
                            modifier         = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (weekIsPast) AccentGreen.copy(alpha = 0.12f)
                                    else wColor.copy(alpha = if (weekIsCurrent) 0.15f else 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (weekIsPast) {
                                Icon(Icons.Default.Check, null, tint = AccentGreen.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
                            } else {
                                Icon(workoutIcon(type), null, tint = wColor.copy(alpha = if (weekIsCurrent) 0.8f else 0.3f), modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }

                // Status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            when {
                                weekIsCurrent -> color.copy(alpha = 0.15f)
                                weekIsPast    -> AccentGreen.copy(alpha = 0.1f)
                                else          -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text      = when {
                            weekIsCurrent -> "Active"
                            weekIsPast    -> "Done"
                            else          -> "upcoming"
                        },
                        color     = when {
                            weekIsCurrent -> color
                            weekIsPast    -> AccentGreen
                            else          -> Color.White.copy(alpha = 0.15f)
                        },
                        fontSize  = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TAB 3 — FULL PLAN
// ─────────────────────────────────────────────

@Composable
private fun FullPlanTab(
    plan: com.application.polarapplication.ai.model.TrainingPlan,
    today: LocalDate,
    progressFraction: Float,
    currentWeekNum: Int,
    totalWeeks: Int,
    planStart: LocalDate,
    competitionDate: LocalDate,
    onGenerateNewPlan: () -> Unit,
    viewModel: DashboardViewModel
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progressFraction,
        animationSpec = tween(1000),
        label         = "macroProgress"
    )
    val fmt     = DateTimeFormatter.ofPattern("MMM dd")
    val fmtFull = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    var expandedBlock by remember { mutableStateOf<Int?>(null) }

    // Overall progress
    SectionLabel("OVERALL PROGRESS")
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("24-Week Plan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Week $currentWeekNum · ${(progressFraction * 100).toInt()}%",
                color      = AccentIndigo,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Segmented progress bar
        val totalDays = ChronoUnit.DAYS.between(planStart, competitionDate).toFloat().coerceAtLeast(1f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            plan.mesoCycles.forEach { meso ->
                val days     = ChronoUnit.DAYS.between(meso.startDate, meso.endDate).toFloat()
                val fraction = (days / totalDays)
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(phaseColor(meso.phase).copy(alpha = 0.3f))
                )
            }
        }

        // Progress overlay
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.03f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentIndigo)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(planStart.format(fmt), color = Color.White.copy(alpha = 0.2f), fontSize = 9.sp)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
                Text(competitionDate.format(fmtFull), color = AccentRed.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // All blocks
    SectionLabel("ALL TRAINING BLOCKS")
    Spacer(modifier = Modifier.height(6.dp))

    plan.mesoCycles.forEachIndexed { index, meso ->
        val isActive   = !meso.startDate.isAfter(today) && meso.endDate.isAfter(today)
        val isPast     = meso.endDate.isBefore(today)
        val isExpanded = expandedBlock == index
        val color      = phaseColor(meso.phase)
        val weeksToStart = ChronoUnit.WEEKS.between(today, meso.startDate).coerceAtLeast(0)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isActive -> color.copy(alpha = 0.08f)
                        isPast   -> Color(0xFF0D0D0D)
                        else     -> Color(0xFF111118)
                    }
                )
                .border(
                    if (isActive) 1.5.dp else 0.5.dp,
                    when {
                        isActive -> color.copy(alpha = 0.3f)
                        isPast   -> Color.White.copy(alpha = 0.05f)
                        else     -> Color.White.copy(alpha = 0.08f)
                    },
                    RoundedCornerShape(14.dp)
                )
                .clickable { expandedBlock = if (isExpanded) null else index }
                .padding(12.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = if (isPast) 0.3f else 1f))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        blockName(meso.phase),
                        color      = Color.White.copy(alpha = if (isPast) 0.4f else 1f),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${meso.startDate.format(fmt)} → ${meso.endDate.format(fmt)} · ${meso.microCycle.size} weeks",
                        color    = color.copy(alpha = if (isPast) 0.3f else 0.6f),
                        fontSize = 10.sp
                    )
                }
                when {
                    isActive -> Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("ACTIVE", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                    isPast -> Icon(Icons.Default.CheckCircle, null, tint = AccentGreen.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    else   -> Text(
                        "in $weeksToStart wks",
                        color    = Color.White.copy(alpha = 0.2f),
                        fontSize = 10.sp
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint     = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expanded desc
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(blockDesc(meso.phase), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Action buttons
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick  = onGenerateNewPlan,
            modifier = Modifier.weight(1f).height(46.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.1f)),
            shape    = RoundedCornerShape(12.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.25f))
        ) {
            Icon(Icons.Default.EditCalendar, null, tint = AccentIndigo, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Change Date", color = AccentIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick  = {
                viewModel.setCompetitionDate(LocalDate.now().plusWeeks(24))
            },
            modifier = Modifier.weight(1f).height(46.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.06f)),
            shape    = RoundedCornerShape(12.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.DeleteOutline, null, tint = AccentRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Plan", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────
// SMALL COMPONENTS
// ─────────────────────────────────────────────

@Composable
private fun InfoChip(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        color         = Color.White.copy(alpha = 0.25f),
        fontSize      = 9.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}