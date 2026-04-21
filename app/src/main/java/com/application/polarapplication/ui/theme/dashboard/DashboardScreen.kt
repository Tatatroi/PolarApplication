package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState
import com.application.polarapplication.ai.planning.TrainingPlanner
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfoData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)

private fun workoutColor(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> Color(0xFF818CF8)
    WorkoutType.ENDURANCE -> Color(0xFF4ADE80)
    WorkoutType.SPEED -> Color(0xFFFBBF24)
    WorkoutType.RECOVERY -> Color(0xFF60A5FA)
    WorkoutType.REST -> Color(0xFF666677)
}

private fun workoutBgColor(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> Color(0x1A818CF8)
    WorkoutType.ENDURANCE -> Color(0x1A4ADE80)
    WorkoutType.SPEED -> Color(0x1AFBBF24)
    WorkoutType.RECOVERY -> Color(0x1A60A5FA)
    WorkoutType.REST -> Color(0x1A666677)
}

private fun workoutLabel(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "STRENGTH"
    WorkoutType.ENDURANCE -> "ENDURANCE"
    WorkoutType.SPEED -> "SPEED"
    WorkoutType.RECOVERY -> "RECOVERY"
    WorkoutType.REST -> "REST"
}

private fun workoutName(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "Forță Maximă"
    WorkoutType.ENDURANCE -> "Rezistență Aerobă"
    WorkoutType.SPEED -> "Viteză Explozivă"
    WorkoutType.RECOVERY -> "Recuperare Activă"
    WorkoutType.REST -> "Zi de Odihnă"
}

private fun workoutDuration(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "45–60 min"
    WorkoutType.ENDURANCE -> "30–45 min"
    WorkoutType.SPEED -> "30–40 min"
    WorkoutType.RECOVERY -> "20–30 min"
    WorkoutType.REST -> "—"
}

private fun workoutIntensity(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH -> "80–85% 1RM · 5×5"
    WorkoutType.ENDURANCE -> "65–75% HRmax"
    WorkoutType.SPEED -> "10×20 sec sprint"
    WorkoutType.RECOVERY -> "sub 65% HRmax"
    WorkoutType.REST -> "Odihnă totală"
}

private fun cnsHint(cnsScore: Int, workoutType: WorkoutType): String {
    if (workoutType == WorkoutType.REST) return "Zi de odihnă — recuperare completă"
    return when {
        cnsScore >= 70 -> "CNS odihnit — antrenament intens recomandat"
        cnsScore >= 50 -> "CNS normal — antrenament standard recomandat"
        cnsScore > 0 -> "CNS obosit — prioritizează recuperarea"
        else -> "Conectează senzorul pentru analiza CNS"
    }
}

private fun cnsHintColor(cnsScore: Int): Color = when {
    cnsScore >= 70 -> Color(0xFF4ADE80)
    cnsScore >= 50 -> Color(0xFFFBBF24)
    cnsScore > 0 -> Color(0xFFF87171)
    else -> Color(0xFF555566)
}

private fun phaseColor(phase: String) = when (phase.lowercase()) {
    "general" -> Color(0xFF4ADE80)
    "specific" -> Color(0xFFFBBF24)
    "precomp" -> Color(0xFFA78BFA)
    "comp" -> Color(0xFFF87171)
    "recovery" -> Color(0xFF67E8F9)
    else -> Color(0xFF818CF8)
}

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onMaximizeWorkout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val competitionDate by viewModel.competitionDate.collectAsState()
    val planStartDate by viewModel.planStartDate.collectAsState()
    val today = remember { LocalDate.now() }

    val effectiveComp = competitionDate ?: today.plusWeeks(24)
    val effectiveStart = planStartDate ?: today

    val planner = remember { TrainingPlanner() }
    val plan = remember(effectiveStart, effectiveComp) {
        planner.generatePlan(effectiveComp, effectiveStart)
    }

    val totalWeeks = plan.mesoCycles.sumOf { it.microCycle.size }

    val currentMeso = plan.mesoCycles.firstOrNull { meso ->
        meso.microCycle.any { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
    } ?: plan.mesoCycles.firstOrNull()

    val currentMicro = currentMeso?.microCycle?.firstOrNull {
        !it.startDate.isAfter(today) && !it.endDate.isBefore(today)
    } ?: currentMeso?.microCycle?.firstOrNull()

    val todayWorkoutType = run {
        val dayIndex = (today.dayOfWeek.value - 1).coerceIn(0, 6)
        currentMicro?.workouts?.getOrNull(dayIndex) ?: WorkoutType.REST
    }

    val currentWeekNum = plan.mesoCycles.flatMap { it.microCycle }
        .indexOfFirst { it.startDate == currentMicro?.startDate }
        .plus(1).coerceAtLeast(1)

    val weeksLeftInPhase = currentMeso?.microCycle
        ?.count { it.startDate.isAfter(today) || it.startDate == today }
        ?.coerceAtLeast(0) ?: 0

    val daysToComp = ChronoUnit.DAYS.between(today, effectiveComp).coerceAtLeast(0)

    // Blob color reactioneaza la puls
    val hrPct = uiState.vitals.heartRate.toFloat() / 200f
    val blobColor by animateColorAsState(
        targetValue = when {
            hrPct >= 0.9f -> Color(0x0DEF4444)
            hrPct >= 0.8f -> Color(0x0DF97316)
            hrPct >= 0.7f -> Color(0x0D4ADE80)
            hrPct >= 0.6f -> Color(0x0D60A5FA)
            else -> Color(0x0D6366F1)
        },
        animationSpec = tween(1000),
        label = "blob"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Blob decorativ fundal
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 100.dp, y = (-80).dp)
                .background(Brush.radialGradient(listOf(blobColor, Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-50).dp, y = 380.dp)
                .background(Brush.radialGradient(listOf(Color(0x054ADE80), Color.Transparent)), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            DashHeader(
                daysToComp = daysToComp,
                phaseName = currentMeso?.phase ?: "—",
                currentWeekNum = currentWeekNum,
                totalWeeks = totalWeeks,
                competitionDate = effectiveComp
            )

            Spacer(modifier = Modifier.height(14.dp))

            SensorCard(
                device = uiState.device,
                vitals = uiState.vitals,
                onDisconnectClick = {
                    if (uiState.device.deviceId.isNotEmpty()) {
                        viewModel.toggleConnection(uiState.device.deviceId)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Antrenament activ — mini preview
            if (uiState.device.isConnected && uiState.isWorkoutActive) {
                ActiveSessionCard(
                    vitals = uiState.vitals,
                    workoutType = workoutLabel(todayWorkoutType),
                    onMaximizeWorkout = onMaximizeWorkout
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else if (uiState.device.isConnected) {
                WorkoutControlPanel(
                    isActive = uiState.isWorkoutActive,
                    vitals = uiState.vitals,
                    onStart = { viewModel.startWorkout() },
                    onStop = { type -> viewModel.stopWorkout(type) },
                    onMaximizeWorkout = onMaximizeWorkout,
                    onTypeSelected = { type -> viewModel.setWorkoutType(type) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            CnsCard(vitals = uiState.vitals)

            Spacer(modifier = Modifier.height(10.dp))

            if (competitionDate != null) {
                PhaseCompactCard(
                    phaseName = currentMeso?.phase ?: "—",
                    weeksLeft = weeksLeftInPhase,
                    weekNum = currentWeekNum,
                    totalWeeks = totalWeeks,
                    competitionDate = effectiveComp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            TodayWorkoutCard(
                workoutType = todayWorkoutType,
                cnsScore = uiState.vitals.cnsScore,
                isConnected = uiState.device.isConnected
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────

@Composable
private fun DashHeader(
    daysToComp: Long,
    phaseName: String,
    currentWeekNum: Int,
    totalWeeks: Int,
    competitionDate: LocalDate
) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text("Salut, Stefan", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("stefanmitroi@gmail.com", color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(phaseColor(phaseName)))
                Text(
                    text = "${phaseName.replaceFirstChar { it.uppercase() }} · S$currentWeekNum/$totalWeeks",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (daysToComp > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AF87171))
                    .border(1.dp, Color(0x33F87171), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$daysToComp", color = Color(0xFFF87171), fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                    Text("zile", color = Color(0x99F87171), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SENSOR CARD
// ─────────────────────────────────────────────

@Composable
private fun SensorCard(
    device: DeviceState,
    vitals: AthleteVitals,
    onDisconnectClick: () -> Unit
) {
    val hrPct = (vitals.heartRate.toFloat() / 200f).coerceIn(0f, 1f)
    val zoneColor by animateColorAsState(
        targetValue = if (device.isConnected) {
            when {
                hrPct >= 0.9f -> Color(0xFFEF4444)
                hrPct >= 0.8f -> Color(0xFFF97316)
                hrPct >= 0.7f -> Color(0xFF4ADE80)
                hrPct >= 0.6f -> Color(0xFF60A5FA)
                else -> Color(0xFF4ADE80)
            }
        } else {
            Color.White.copy(alpha = 0.2f)
        },
        animationSpec = tween(500),
        label = "zoneColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = device.isConnected) { onDisconnectClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(zoneColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (device.isConnected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = zoneColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            if (device.isConnected) {
                Text("SENZOR ACTIV", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("${vitals.heartRate} BPM", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                Text("Apasă pentru deconectare", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
            } else {
                Text("Așteptare conexiune...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Configurează din tab-ul Senzori", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
            }
        }

        if (device.isConnected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF4ADE80).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Live", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.2f))
        }
    }
}

// ─────────────────────────────────────────────
// SESIUNE ACTIVĂ MINI
// ─────────────────────────────────────────────

@Composable
private fun ActiveSessionCard(
    vitals: AthleteVitals,
    workoutType: String,
    onMaximizeWorkout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AF97316))
            .border(1.dp, Color(0x33F97316), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("SESIUNE ACTIVĂ", color = Color(0xFFF97316), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(workoutType, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TRIMP ${"%.1f".format(vitals.trimpScore)}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                Text("${vitals.calories} kcal", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        }
        Button(
            onClick = onMaximizeWorkout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F97316)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("Live →", color = Color(0xFFF97316), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────
// CNS CARD
// ─────────────────────────────────────────────

@Composable
private fun CnsCard(vitals: AthleteVitals) {
    val cns = vitals.cnsScore.coerceIn(0, 100)
    val cnsColor by animateColorAsState(
        targetValue = when {
            cns >= 70 -> Color(0xFF4ADE80)
            cns >= 50 -> Color(0xFFFBBF24)
            cns > 0 -> Color(0xFFF87171)
            else -> Color.White.copy(alpha = 0.2f)
        },
        animationSpec = tween(500),
        label = "cnsColor"
    )
    val cnsLabel = when {
        cns >= 70 -> "ODIHNIT"
        cns >= 50 -> "NORMAL"
        cns > 0 -> "OBOSIT"
        else -> "N/A"
    }
    val animatedFrac by animateFloatAsState(targetValue = cns / 100f, animationSpec = tween(800), label = "cnsFrac")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CNS READINESS", color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    InfoIconButton(info = MetricInfoData.CNS, tint = Color.White.copy(alpha = 0.15f))
                }
                Text("based on live RMSSD", color = Color.White.copy(alpha = 0.18f), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (cns > 0) "$cns" else "—", color = cnsColor, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp)
                Text(cnsLabel, color = cnsColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(modifier = Modifier.fillMaxWidth(animatedFrac).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(cnsColor))
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Epuizat", "Obosit", "Normal", "Odihnit").forEach { lbl ->
                Text(lbl, color = Color.White.copy(alpha = 0.12f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────
// FAZA BOMPA COMPACT
// ─────────────────────────────────────────────

@Composable
private fun PhaseCompactCard(
    phaseName: String,
    weeksLeft: Int,
    weekNum: Int,
    totalWeeks: Int,
    competitionDate: LocalDate
) {
    val color = phaseColor(phaseName)
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
            Column {
                Text("Faza ${phaseName.replaceFirstChar { it.uppercase() }}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("$weeksLeft săpt. rămase în fază", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("S$weekNum / $totalWeeks", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(competitionDate.format(fmt), color = Color(0xFF4ADE80).copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

// ─────────────────────────────────────────────
// ANTRENAMENTUL DE AZI
// ─────────────────────────────────────────────

@Composable
private fun TodayWorkoutCard(
    workoutType: WorkoutType,
    cnsScore: Int,
    isConnected: Boolean
) {
    val color = workoutColor(workoutType)
    val bgColor = workoutBgColor(workoutType)
    val hintColor = if (isConnected) cnsHintColor(cnsScore) else Color.White.copy(alpha = 0.2f)
    val hintText = if (isConnected) cnsHint(cnsScore, workoutType) else "Conectează senzorul pentru recomandarea CNS"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AZI · PLAN BOMPA", color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(workoutLabel(workoutType), color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(workoutName(workoutType), color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(12.dp))

        if (workoutType != WorkoutType.REST) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(10.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(hintColor.copy(alpha = 0.07f))
                .border(0.5.dp, hintColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default
                        .Psychology,
                    contentDescription = null,
                    tint = hintColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(hintText, color = hintColor, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun TodayDetailRow(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 13.sp) }
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        Text(value, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}