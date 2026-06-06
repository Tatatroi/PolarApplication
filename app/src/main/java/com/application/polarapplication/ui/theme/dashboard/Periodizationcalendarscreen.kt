package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.planning.MesoCycle
import com.application.polarapplication.ai.planning.MicroCycle
import com.application.polarapplication.ai.planning.TrainingPlanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// CULORI TEMĂ
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF0D0D12)
private val CardDark = Color(0xFF15151C)
private val BorderDark = Color(0xFF1E1E2E)

private val ColorGeneral = Color(0xFF4ADE80) // verde
private val ColorSpecific = Color(0xFFFBBF24) // galben
private val ColorPrecomp = Color(0xFFA78BFA) // violet deschis
private val ColorComp = Color(0xFFF87171) // roșu
private val ColorRecovery = Color(0xFF67E8F9) // cyan

private val ColorSTR = Color(0xFF818CF8)
private val ColorEND = Color(0xFF4ADE80)
private val ColorSPD = Color(0xFFFBBF24)
private val ColorREC = Color(0xFF60A5FA)
private val ColorREST = Color(0xFF444444)
private val ColorIndigo = Color(0xFF6366F1)

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

private fun phaseColor(phaseName: String): Color = when (phaseName.lowercase()) {
    "general" -> ColorGeneral
    "specific" -> ColorSpecific
    "precomp" -> ColorPrecomp
    "comp" -> ColorComp
    "recovery" -> ColorRecovery
    else -> Color.Gray
}

private fun phaseBg(phaseName: String): Color = when (phaseName.lowercase()) {
    "general" -> Color(0xFF1D3A2A)
    "specific" -> Color(0xFF2A1F00)
    "precomp" -> Color(0xFF1A0D2E)
    "comp" -> Color(0xFF1A0808)
    "recovery" -> Color(0xFF0D1A1A)
    else -> Color(0xFF1A1A24)
}

private fun workoutColor(type: WorkoutType): Color = when (type) {
    WorkoutType.STRENGTH -> ColorSTR
    WorkoutType.ENDURANCE -> ColorEND
    WorkoutType.SPEED -> ColorSPD
    WorkoutType.RECOVERY -> ColorREC
    WorkoutType.REST -> ColorREST
}

private fun workoutBg(type: WorkoutType): Color = when (type) {
    WorkoutType.STRENGTH -> Color(0xFF1A1A3E)
    WorkoutType.ENDURANCE -> Color(0xFF0F2A1A)
    WorkoutType.SPEED -> Color(0xFF2A1F00)
    WorkoutType.RECOVERY -> Color(0xFF0A1A2A)
    WorkoutType.REST -> Color(0xFF1A1A1A)
}

private fun workoutLabel(type: WorkoutType): String = when (type) {
    WorkoutType.STRENGTH -> "STR"
    WorkoutType.ENDURANCE -> "END"
    WorkoutType.SPEED -> "SPD"
    WorkoutType.RECOVERY -> "REC"
    WorkoutType.REST -> "REST"
}

private fun phaseDescription(phaseName: String): String = when (phaseName.lowercase()) {
    "general" -> "Builds aerobic base and general strength. High volume, moderate intensity."
    "specific" -> "Transfers capacities toward sport demands. Increased intensity."
    "precomp" -> "Competition simulation. Low volume, maximum intensity."
    "comp" -> "Maintaining peak form. Short and explosive sessions."
    "recovery" -> "Complete neuromuscular regeneration. Preparation for next macrocycle."
    else -> ""
}

private fun workoutDescription(type: WorkoutType): String = when (type) {
    WorkoutType.STRENGTH -> "Strength: 5×5 at 80–85% 1RM. 3 min rest between sets."
    WorkoutType.ENDURANCE -> "Aerobic endurance: 30–40 min at 65–75% HRmax. Steady pace."
    WorkoutType.SPEED -> "Speed: 10×20 sec maximal sprint, 40 sec active rest."
    WorkoutType.RECOVERY -> "Active recovery: 20–25 min easy walk + mobility work."
    WorkoutType.REST -> "Full rest day. Prioritize sleep and optimal hydration."
}

data class SelectedDayInfo(
    val date: LocalDate,
    val workoutType: WorkoutType,
    val microCycle: MicroCycle,
    val mesoCycle: MesoCycle,
    val weekNumberInPlan: Int
)

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun PeriodizationCalendarScreen(
    viewModel: DashboardViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val competitionDate by viewModel.competitionDate.collectAsState()
    val effectiveDate = competitionDate ?: LocalDate.now().plusWeeks(24)

    val availableDays by viewModel.profileManager.availableDays.collectAsState()
    val planFocus by viewModel.planFocus.collectAsState()

    val planner = remember { TrainingPlanner() }
    val plan = remember(effectiveDate, availableDays, planFocus) {
        planner.generatePlan(
            competitionDate = effectiveDate,
            availableDays = availableDays,
            focus = planFocus
        )
    }
    val today = remember { LocalDate.now() }

    // Găsim microciclu-ul săptămânii curente
    val currentMeso = remember(plan) {
        plan.mesoCycles.firstOrNull { meso ->
            meso.microCycle.any { micro ->
                !micro.startDate.isAfter(today) && !micro.endDate.isBefore(today)
            }
        }
    }
    val currentMicro = remember(currentMeso) {
        currentMeso?.microCycle?.firstOrNull { micro ->
            !micro.startDate.isAfter(today) && !micro.endDate.isBefore(today)
        }
    }

    // State-uri
    var selectedMeso by remember { mutableStateOf(currentMeso ?: plan.mesoCycles.firstOrNull()) }
    var selectedMicro by remember { mutableStateOf(currentMicro ?: plan.mesoCycles.firstOrNull()?.microCycle?.firstOrNull()) }
    var selectedDayInfo by remember { mutableStateOf<SelectedDayInfo?>(null) }

    val totalWeeks = plan.mesoCycles.sumOf { it.microCycle.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Header ──
        PlanHeader(
            competitionDate = effectiveDate,
            totalWeeks = totalWeeks,
            today = today
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Legendă ──
        PhaseLegend()

        Spacer(modifier = Modifier.height(16.dp))

        // ── Timeline Macro + Mezo ──
        SectionLabel("Timeline plan")
        Spacer(modifier = Modifier.height(8.dp))
        PeriodizationTimeline(
            plan = plan,
            today = today,
            competitionDate = effectiveDate,
            selectedMeso = selectedMeso,
            onMesoClick = { meso ->
                selectedMeso = meso
                selectedMicro = meso.microCycle.firstOrNull()
                selectedDayInfo = null
            },
            onMacroClick = {
                selectedMeso = null
                selectedMicro = currentMicro ?: plan.mesoCycles.firstOrNull()?.microCycle?.firstOrNull()
                selectedDayInfo = null
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedMeso == null) {
            // Arată toate săptămânile din toate fazele
            SectionLabel("FULL PLAN · ${plan.mesoCycles.sumOf { it.microCycle.size }} weeks")
            Spacer(modifier = Modifier.height(8.dp))
            MicroCycleSelector(
                mesoCycles = plan.mesoCycles,  // toate fazele
                selectedMicro = selectedMicro,
                today = today,
                totalWeeks = totalWeeks,
                planStart = plan.stratDate,
                onMicroSelected = { micro ->
                    selectedMicro = micro
                    selectedDayInfo = null
                }
            )
        } else {
            SectionLabel("${selectedMeso!!.phase.replaceFirstChar { it.uppercase() }} phase · ${selectedMeso!!.microCycle.size} weeks")
            Spacer(modifier = Modifier.height(8.dp))
            MicroCycleSelector(
                mesoCycles = listOf(selectedMeso!!),
                selectedMicro = selectedMicro,
                today = today,
                totalWeeks = totalWeeks,
                planStart = plan.stratDate,
                onMicroSelected = { micro ->
                    selectedMicro = micro
                    selectedDayInfo = null
                }
            )
        }


        Spacer(modifier = Modifier.height(20.dp))

        selectedMicro?.let { micro ->
            val mesoForMicro = selectedMeso ?: plan.mesoCycles.firstOrNull { meso ->
                meso.microCycle.any { it.startDate == micro.startDate }
            } ?: return@let

            val weekNum = plan.mesoCycles
                .flatMap { it.microCycle }
                .indexOfFirst { it.startDate == micro.startDate } + 1

            SectionLabel("Microcycle · Week $weekNum of $totalWeeks")
            Spacer(modifier = Modifier.height(8.dp))
            WeekDayGrid(
                microCycle = micro,
                today = today,
                selectedDayInfo = selectedDayInfo,
                onDayClick = { date, type, dayIndex ->
                    selectedDayInfo = SelectedDayInfo(
                        date = date,
                        workoutType = type,
                        microCycle = micro,
                        mesoCycle = mesoForMicro,
                        weekNumberInPlan = weekNum
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Panou detalii zi selectată ──
        AnimatedVisibility(
            visible = selectedDayInfo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedDayInfo?.let { info ->
                DayDetailPanel(
                    info = info,
                    macrocycleLabel = "${plan.mesoCycles.firstOrNull()?.phase?.replaceFirstChar { it.uppercase() } ?: "General"} · $totalWeeks weeks"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────
// COMPONENTE
// ─────────────────────────────────────────────

@Composable
private fun PlanHeader(
    competitionDate: LocalDate,
    totalWeeks: Int,
    today: LocalDate
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val daysLeft = ChronoUnit.DAYS.between(today, competitionDate)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Periodization Plan",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Bompa · $totalWeeks weeks",
                color = Color(0xFF555566),
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A0808), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF3A1010), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "COMPETITION",
                        color = ColorComp.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = competitionDate.format(formatter),
                        color = ColorComp,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$daysLeft days left",
                        color = ColorComp.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseLegend() {
    val phases = listOf(
        "General" to ColorGeneral,
        "Specific" to ColorSpecific,
        "Precomp" to ColorPrecomp,
        "Comp" to ColorComp,
        "Recovery" to ColorRecovery
    )
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        phases.forEach { (name, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = 0.7f))
                )
                Text(text = name, color = Color(0xFF666677), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        // Marker competiție
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ColorComp)
            )
            Text(text = "Competition", color = Color(0xFF666677), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PeriodizationTimeline(
    plan: com.application.polarapplication.ai.model.TrainingPlan,
    today: LocalDate,
    competitionDate: LocalDate,
    selectedMeso: MesoCycle?,
    onMesoClick: (MesoCycle) -> Unit,
    onMacroClick: () -> Unit
) {
    val totalDays = ChronoUnit.DAYS.between(plan.stratDate, plan.endDate).toFloat().coerceAtLeast(1f)
    val todayOffset = ChronoUnit.DAYS.between(plan.stratDate, today).toFloat().coerceIn(0f, totalDays)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(14.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // ── Rând MACRO ──────────────────────────────────────────────────────
        TimelineRow(label = "MACRO") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A3E))
                    .border(1.dp, Color(0xFF2A2A5E), RoundedCornerShape(6.dp))
                    .clickable { onMacroClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Macrocycle",
                    color = ColorIndigo.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                // Today line în macro
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = (todayOffset / totalDays * 100).coerceIn(0f, 99f).dp * 1.5f)
                        .background(ColorIndigo.copy(alpha = 0.6f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Rând MEZO ───────────────────────────────────────────────────────
        TimelineRow(label = "MEZO") {
            Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    plan.mesoCycles.forEach { meso ->
                        val days = ChronoUnit.DAYS.between(meso.startDate, meso.endDate).toFloat()
                        val fraction = days / totalDays
                        val isSelected = meso.startDate == selectedMeso?.startDate
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(phaseBg(meso.phase))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) phaseColor(meso.phase) else phaseColor(meso.phase).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .clickable { onMesoClick(meso) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meso.phase.replaceFirstChar { it.uppercase() },
                                color = phaseColor(meso.phase),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Today line peste mezo
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = ((todayOffset / totalDays) * 300).dp)
                        .background(ColorIndigo)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        MonthLabels(planStart = plan.stratDate, totalDays = totalDays.toInt())
    }
}

@Composable
private fun TimelineRow(
    label: String,
    content: @Composable BoxScope.(Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = Color(0xFF444455),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Box(modifier = Modifier.weight(1f).height(20.dp)) {
            content(0f)
        }
    }
}

@Composable
private fun BoxScope.TodayLine(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.5.dp)
            .align(Alignment.CenterStart)
            .offset(x = fraction.dp * 300) // offset relativ
            .background(ColorIndigo)
    )
    // Punct sus
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(ColorIndigo)
            .align(Alignment.TopStart)
    )
}

@Composable
private fun BoxScope.CompMarker(fraction: Float) {
    // Triunghi/flag competiție simplu cu un Box colorat
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = (fraction * 100).coerceIn(0f, 95f).dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 8.dp, height = 14.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(ColorComp)
        )
    }
}

@Composable
private fun MonthLabels(planStart: LocalDate, totalDays: Int) {
    val months = mutableListOf<Pair<LocalDate, Float>>()
    var cur = planStart.withDayOfMonth(1)
    while (cur.isBefore(planStart.plusDays(totalDays.toLong()))) {
        val offset = ChronoUnit.DAYS.between(planStart, cur).toFloat().coerceAtLeast(0f)
        months.add(cur to offset / totalDays)
        cur = cur.plusMonths(1)
    }
    Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        months.forEach { (date, frac) ->
            val monthNames = listOf("Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Nov", "Dec")
            Text(
                text = monthNames[date.monthValue - 1],
                color = Color(0xFF333344),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (frac * 100).coerceIn(0f, 90f).dp)
            )
        }
    }
}

@Composable
private fun MicroCycleSelector(
    mesoCycles: List<MesoCycle>,  // <-- schimbat
    selectedMicro: MicroCycle?,
    today: LocalDate,
    totalWeeks: Int,
    planStart: LocalDate,
    onMicroSelected: (MicroCycle) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        mesoCycles.forEach { meso ->
            meso.microCycle.forEachIndexed { index, micro ->
                val isCurrentWeek = !micro.startDate.isAfter(today) && !micro.endDate.isBefore(today)
                val isSelected = micro.startDate == selectedMicro?.startDate
                val weekNum = ChronoUnit.WEEKS.between(planStart, micro.startDate).toInt() + 1

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) phaseBg(meso.phase) else Color(0xFF15151C)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = when {
                                isSelected -> phaseColor(meso.phase)
                                isCurrentWeek -> ColorIndigo.copy(alpha = 0.5f)
                                else -> BorderDark
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onMicroSelected(micro) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "S$weekNum",
                            color = if (isSelected) phaseColor(meso.phase) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentWeek) {
                            Text("today", color = ColorIndigo, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayGrid(
    microCycle: MicroCycle,
    today: LocalDate,
    selectedDayInfo: SelectedDayInfo?,
    onDayClick: (LocalDate, WorkoutType, Int) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(14.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Header zile
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    color = Color(0xFF444455),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Celule zile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            microCycle.workouts.forEachIndexed { index, workoutType ->
                val date = microCycle.startDate.plusDays(index.toLong())
                val isToday = date == today
                val isSelected = selectedDayInfo?.date == date

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> workoutBg(workoutType)
                                isToday -> Color(0xFF1A1A2E)
                                else -> Color(0xFF111118)
                            }
                        )
                        .border(
                            width = when {
                                isSelected -> 1.5.dp
                                isToday -> 1.dp
                                else -> 0.5.dp
                            },
                            color = when {
                                isSelected -> workoutColor(workoutType)
                                isToday -> ColorIndigo.copy(alpha = 0.6f)
                                else -> BorderDark
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onDayClick(date, workoutType, index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            color = if (isToday) Color.White else Color(0xFF666677),
                            fontSize = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = workoutLabel(workoutType),
                            color = workoutColor(workoutType),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailPanel(info: SelectedDayInfo, macrocycleLabel: String) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")
    val dayOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val dayName = dayOfWeek[(info.date.dayOfWeek.value - 1)]
    val dateStr = "$dayName, ${info.date.dayOfMonth} ${months[info.date.monthValue - 1]} ${info.date.year}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(16.dp))
            .border(1.dp, workoutColor(info.workoutType).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateStr,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Week ${info.weekNumberInPlan} of plan",
                    color = Color(0xFF555566),
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(workoutBg(info.workoutType))
                    .border(1.dp, workoutColor(info.workoutType).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = workoutLabel(info.workoutType),
                    color = workoutColor(info.workoutType),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // Rânduri detalii
        DetailRow("Mesocycle", "${info.mesoCycle.phase.replaceFirstChar { it.uppercase() }} · ${info.mesoCycle.microCycle.size} weeks")
        DetailRow("Macrocycle", macrocycleLabel)
        Spacer(modifier = Modifier.height(12.dp))

        // Descriere fază
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(phaseBg(info.mesoCycle.phase))
                .border(0.5.dp, phaseColor(info.mesoCycle.phase).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "Phase ${info.mesoCycle.phase.replaceFirstChar { it.uppercase() }}",
                    color = phaseColor(info.mesoCycle.phase),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phaseDescription(info.mesoCycle.phase),
                    color = Color(0xFF888899),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recomandare antrenament
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(workoutBg(info.workoutType))
                .border(0.5.dp, workoutColor(info.workoutType).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = "Bompa Recommendation",
                    color = workoutColor(info.workoutType),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workoutDescription(info.workoutType),
                    color = Color(0xFF888899),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF555566), fontSize = 12.sp)
        Text(text = value, color = Color(0xFFCCCCDD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF444455),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}
