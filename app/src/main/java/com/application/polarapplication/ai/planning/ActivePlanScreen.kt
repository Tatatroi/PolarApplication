package com.application.polarapplication.ai.planning

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF0D0D12)
private val CardDark = Color(0xFF15151C)
private val BorderDark = Color(0xFF1E1E2E)
private val ColorIndigo = Color(0xFF6366F1)

private fun phaseColor(phase: String) = when (phase.lowercase()) {
    "general" -> Color(0xFF4ADE80)
    "specific" -> Color(0xFFFBBF24)
    "precomp" -> Color(0xFFA78BFA)
    "comp" -> Color(0xFFF87171)
    "recovery" -> Color(0xFF67E8F9)
    else -> Color.Gray
}

private fun phaseBg(phase: String) = when (phase.lowercase()) {
    "general" -> Color(0xFF1D3A2A)
    "specific" -> Color(0xFF2A1F00)
    "precomp" -> Color(0xFF1A0D2E)
    "comp" -> Color(0xFF1A0808)
    "recovery" -> Color(0xFF0D1A1A)
    else -> CardDark
}

private fun phaseDesc(phase: String) = when (phase.lowercase()) {
    "general" -> "Volum ridicat, intensitate moderată. Focus pe rezistență aerobă și forță de bază."
    "specific" -> "Transfer spre cerințele sportului. Intensitate crescută, volum moderat."
    "precomp" -> "Simulare competițională. Volum scăzut, intensitate maximă."
    "comp" -> "Menținerea formei de vârf. Antrenamente scurte și explosive."
    "recovery" -> "Regenerare completă neuromusculară. Pregătire pentru următorul ciclu."
    else -> ""
}

private fun workoutColor(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> Color(0xFF818CF8)
    WorkoutType.ENDURANCE -> Color(0xFF4ADE80)
    WorkoutType.SPEED -> Color(0xFFFBBF24)
    WorkoutType.RECOVERY -> Color(0xFF60A5FA)
    WorkoutType.REST -> Color(0xFF444455)
}

private fun workoutBg(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> Color(0xFF1A1A3E)
    WorkoutType.ENDURANCE -> Color(0xFF0F2A1A)
    WorkoutType.SPEED -> Color(0xFF2A1F00)
    WorkoutType.RECOVERY -> Color(0xFF0A1A2A)
    WorkoutType.REST -> Color(0xFF1A1A1A)
}

private fun workoutLabel(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "STR"
    WorkoutType.ENDURANCE -> "END"
    WorkoutType.SPEED -> "SPD"
    WorkoutType.RECOVERY -> "REC"
    WorkoutType.REST -> "REST"
}

private fun workoutIcon(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "💪"
    WorkoutType.ENDURANCE -> "🏃"
    WorkoutType.SPEED -> "⚡"
    WorkoutType.RECOVERY -> "💧"
    WorkoutType.REST -> "—"
}

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun ActivePlanScreen(
    viewModel: DashboardViewModel = viewModel(),
    onGenerateNewPlan: () -> Unit
) {
    val competitionDate by viewModel.competitionDate.collectAsState()
    val planStartDate by viewModel.planStartDate.collectAsState()
    val today = remember { LocalDate.now() }

    // Dacă nu există plan generat → ecran gol cu buton
    if (competitionDate == null || planStartDate == null) {
        NoPlanScreen(onGenerateNewPlan = onGenerateNewPlan)
        return
    }

    val effectiveStart = planStartDate!!
    val effectiveComp = competitionDate!!

    val planner = remember { TrainingPlanner() }
    val plan = remember(effectiveStart, effectiveComp) {
        planner.generatePlan(effectiveComp, effectiveStart)
    }

    val totalWeeks = plan.mesoCycles.sumOf { it.microCycle.size }
    val totalDays = ChronoUnit.DAYS.between(effectiveStart, effectiveComp).toInt().coerceAtLeast(1)
    val elapsedDays = ChronoUnit.DAYS.between(effectiveStart, today).toInt().coerceIn(0, totalDays)
    val progressFraction = elapsedDays.toFloat() / totalDays.toFloat()
    val currentWeekNum = ((elapsedDays / 7) + 1).coerceIn(1, totalWeeks)

    // Mezo și micro curent
    val currentMeso = plan.mesoCycles.firstOrNull { meso ->
        meso.microCycle.any { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
    } ?: plan.mesoCycles.first()

    val currentMicro = currentMeso.microCycle.firstOrNull {
        !it.startDate.isAfter(today) && !it.endDate.isBefore(today)
    } ?: currentMeso.microCycle.first()

    // Săptămâni rămase în faza curentă
    val weeksRemainingInPhase = currentMeso.microCycle
        .count { it.startDate.isAfter(today) || it.startDate == today }
        .coerceAtLeast(0)

    val daysToComp = ChronoUnit.DAYS.between(today, effectiveComp).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Header ──
        PlanHeader(
            competitionDate = effectiveComp,
            planStart = effectiveStart,
            daysToComp = daysToComp,
            onNewPlan = onGenerateNewPlan
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Progress Macro ──
        SectionLabel("Macrociclu")
        Spacer(modifier = Modifier.height(8.dp))
        MacroProgressCard(
            planStart = effectiveStart,
            competitionDate = effectiveComp,
            progressFraction = progressFraction,
            currentWeek = currentWeekNum,
            totalWeeks = totalWeeks
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Faza curentă ──
        SectionLabel("Faza curentă")
        Spacer(modifier = Modifier.height(8.dp))
        CurrentPhaseCard(
            meso = currentMeso,
            weeksRemainingInPhase = weeksRemainingInPhase
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Săptămâna curentă ──
        SectionLabel("Săptămâna $currentWeekNum — ${currentMeso.phase.replaceFirstChar { it.uppercase() }}")
        Spacer(modifier = Modifier.height(8.dp))
        CurrentWeekCard(micro = currentMicro, today = today)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Toate fazele ──
        SectionLabel("Toate fazele planului")
        Spacer(modifier = Modifier.height(8.dp))
        plan.mesoCycles.forEach { meso ->
            val isCurrentMeso = meso.startDate == currentMeso.startDate
            PhaseCard(
                meso = meso,
                isCurrent = isCurrentMeso,
                today = today
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────
// ECRAN FĂRĂ PLAN
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
        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(56.dp))

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Niciun plan activ",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generează un plan de periodizare Bompa pe baza datei competiției tale.",
            color = Color(0xFF555566),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onGenerateNewPlan,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorIndigo),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Generează plan nou",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE
// ─────────────────────────────────────────────

@Composable
private fun PlanHeader(
    competitionDate: LocalDate,
    planStart: LocalDate,
    daysToComp: Long,
    onNewPlan: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Plan Activ",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${planStart.format(fmt)} → ${competitionDate.format(fmt)}",
                color = Color(0xFF555566),
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A0808))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$daysToComp",
                        color = Color(0xFFF87171),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "zile rămase",
                        color = Color(0xFFF87171).copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(
                onClick = onNewPlan,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "+ plan nou",
                    color = ColorIndigo.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MacroProgressCard(
    planStart: LocalDate,
    competitionDate: LocalDate,
    progressFraction: Float,
    currentWeek: Int,
    totalWeeks: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(1000),
        label = "macroProgress"
    )
    val fmt = DateTimeFormatter.ofPattern("dd MMM")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Macrociclu Forță",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${(progressFraction * 100).toInt()}% completat",
                color = ColorIndigo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bară progres
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BorderDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorIndigo)
            )
            // Marker azi
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (animatedProgress * 300).dp.coerceAtMost(298.dp))
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = planStart.format(fmt),
                color = Color(0xFF444455),
                fontSize = 10.sp
            )
            Text(
                text = "Săpt. $currentWeek / $totalWeeks",
                color = Color(0xFF666677),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = competitionDate.format(fmt),
                color = Color(0xFFF87171).copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CurrentPhaseCard(
    meso: MesoCycle,
    weeksRemainingInPhase: Int
) {
    val color = phaseColor(meso.phase)
    val bg = phaseBg(meso.phase)
    val fmt = DateTimeFormatter.ofPattern("dd MMM")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Faza ${meso.phase.replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${meso.startDate.format(fmt)} → ${meso.endDate.format(fmt)}",
                color = color.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = phaseDesc(meso.phase),
                color = Color(0xFF777788),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$weeksRemainingInPhase",
                color = color,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 28.sp
            )
            Text(
                text = "săpt.\nrămase",
                color = color.copy(alpha = 0.6f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun CurrentWeekCard(micro: MicroCycle, today: LocalDate) {
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Azi: ${today.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale("ro")
            )}, ${today.dayOfMonth} ${today.month.getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale("ro")
            )}",
            color = Color(0xFF666677),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            micro.workouts.forEachIndexed { index, workoutType ->
                val date = micro.startDate.plusDays(index.toLong())
                val isToday = date == today
                val isPast = date.isBefore(today)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isToday -> workoutBg(workoutType)
                                isPast -> Color(0xFF0D0D12)
                                else -> Color(0xFF111118)
                            }
                        )
                        .border(
                            width = if (isToday) 1.5.dp else 0.5.dp,
                            color = when {
                                isToday -> workoutColor(workoutType)
                                isPast -> Color(0xFF222228)
                                else -> BorderDark
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Numărul zilei
                    Text(
                        text = "${date.dayOfMonth}",
                        color = when {
                            isToday -> Color.White
                            isPast -> Color(0xFF333340)
                            else -> Color(0xFF555566)
                        },
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Icon workout
                    Text(
                        text = workoutIcon(workoutType),
                        fontSize = 14.sp,
                        color = if (isPast && !isToday) {
                            workoutColor(workoutType).copy(alpha = 0.3f)
                        } else {
                            workoutColor(workoutType)
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Label tip
                    Text(
                        text = workoutLabel(workoutType),
                        color = when {
                            isToday -> workoutColor(workoutType)
                            isPast -> workoutColor(workoutType).copy(alpha = 0.3f)
                            else -> workoutColor(workoutType).copy(alpha = 0.7f)
                        },
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Indicator "azi"
                    if (isToday) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(workoutColor(workoutType))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseCard(
    meso: MesoCycle,
    isCurrent: Boolean,
    today: LocalDate
) {
    val color = phaseColor(meso.phase)
    val bg = phaseBg(meso.phase)
    val fmt = DateTimeFormatter.ofPattern("dd MMM")
    val isPast = meso.endDate.isBefore(today)
    val alpha = if (isPast) 0.5f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = if (isPast) 0.5f else 1f))
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) color else color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        // Header fază
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = alpha))
                )
                Text(
                    text = meso.phase.replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorIndigo.copy(alpha = 0.15f))
                            .border(0.5.dp, ColorIndigo.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIV",
                            color = ColorIndigo,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                if (isPast) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF222228))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FINALIZAT",
                            color = Color(0xFF444455),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Text(
                text = "${meso.microCycle.size} săpt.",
                color = color.copy(alpha = alpha * 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Date
        Text(
            text = "${meso.startDate.format(fmt)} → ${meso.endDate.format(fmt)}",
            color = Color(0xFF555566).copy(alpha = alpha),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Descriere
        Text(
            text = phaseDesc(meso.phase),
            color = Color(0xFF666677).copy(alpha = alpha),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Săptămânile fazei ca pill-uri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            meso.microCycle.forEachIndexed { index, micro ->
                val weekIsPast = micro.endDate.isBefore(today)
                val weekIsCurrent = !micro.startDate.isAfter(today) && !micro.endDate.isBefore(today)
                val totalInPhase = meso.microCycle.size
                val weekLabel = "S${index + 1}"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                weekIsCurrent -> color.copy(alpha = 0.2f)
                                weekIsPast -> Color(0xFF111115)
                                else -> color.copy(alpha = 0.06f)
                            }
                        )
                        .border(
                            width = if (weekIsCurrent) 1.dp else 0.5.dp,
                            color = when {
                                weekIsCurrent -> color.copy(alpha = 0.7f)
                                weekIsPast -> Color(0xFF222228)
                                else -> color.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weekLabel,
                        color = when {
                            weekIsCurrent -> color
                            weekIsPast -> Color(0xFF333340)
                            else -> color.copy(alpha = 0.5f)
                        },
                        fontSize = 8.sp,
                        fontWeight = if (weekIsCurrent) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
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
