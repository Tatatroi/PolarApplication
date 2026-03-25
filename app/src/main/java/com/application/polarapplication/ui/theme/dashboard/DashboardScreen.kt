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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.daily.WorkoutType
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState
import com.application.polarapplication.ai.planning.MicroCycle
import com.application.polarapplication.ai.planning.TrainingPlanner
import com.application.polarapplication.ui.theme.Indigo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────
// CULORI LOCALE
// ─────────────────────────────────────
private val BgDark     = Color(0xFF0D0D12)
private val CardDark   = Color(0xFF15151C)
private val BorderDark = Color(0xFF1E1E2E)

private val ColorSTR   = Color(0xFF818CF8)
private val ColorEND   = Color(0xFF4ADE80)
private val ColorSPD   = Color(0xFFFBBF24)
private val ColorREC   = Color(0xFF60A5FA)
private val ColorREST  = Color(0xFF444455)
private val ColorGreen = Color(0xFF4ADE80)

private fun workoutColor(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> ColorSTR
    WorkoutType.ENDURANCE -> ColorEND
    WorkoutType.SPEED     -> ColorSPD
    WorkoutType.RECOVERY  -> ColorREC
    WorkoutType.REST      -> ColorREST
}

private fun workoutBg(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> Color(0xFF1A1A3E)
    WorkoutType.ENDURANCE -> Color(0xFF0F2A1A)
    WorkoutType.SPEED     -> Color(0xFF2A1F00)
    WorkoutType.RECOVERY  -> Color(0xFF0A1A2A)
    WorkoutType.REST      -> Color(0xFF1A1A1A)
}

private fun workoutLabel(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "STR"
    WorkoutType.ENDURANCE -> "END"
    WorkoutType.SPEED     -> "SPD"
    WorkoutType.RECOVERY  -> "REC"
    WorkoutType.REST      -> "REST"
}

private fun workoutIcon(type: WorkoutType) = when (type) {
    WorkoutType.STRENGTH  -> "💪"
    WorkoutType.ENDURANCE -> "🏃"
    WorkoutType.SPEED     -> "⚡"
    WorkoutType.RECOVERY  -> "🔵"
    WorkoutType.REST      -> "—"
}

private fun workoutRecommendation(type: WorkoutType): Triple<String, String, String> =
    when (type) {
        WorkoutType.STRENGTH  -> Triple("Forță", "45–60 min", "80–85% 1RM · 5×5")
        WorkoutType.ENDURANCE -> Triple("Rezistență Aerobă", "30–45 min", "65–75% HRmax · ritm constant")
        WorkoutType.SPEED     -> Triple("Viteză Explozivă", "30–40 min", "10×20 sec sprint · pauze 40 sec")
        WorkoutType.RECOVERY  -> Triple("Recuperare Activă", "20–30 min", "sub 65% HRmax · mobilitate")
        WorkoutType.REST      -> Triple("Odihnă Totală", "—", "somn prioritar · hidratare")
    }

// ─────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onMaximizeWorkout: () -> Unit
) {
    val uiState      by viewModel.uiState.collectAsState()
    val competitionDate by viewModel.competitionDate.collectAsState()
    val effectiveCompDate = competitionDate ?: LocalDate.now().plusWeeks(24)

    // Generăm planul pe baza datei de competiție
    val planner = remember { TrainingPlanner() }
    val plan    = remember(effectiveCompDate) { planner.generatePlan(effectiveCompDate) }
    val today   = remember { LocalDate.now() }

    // Găsim mezociclu și microciclu curent
    val currentMeso = remember(plan) {
        plan.mesoCycles.firstOrNull { meso ->
            meso.microCycle.any { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
        } ?: plan.mesoCycles.firstOrNull()
    }
    val currentMicro = remember(currentMeso) {
        currentMeso?.microCycle?.firstOrNull {
            !it.startDate.isAfter(today) && !it.endDate.isBefore(today)
        } ?: currentMeso?.microCycle?.firstOrNull()
    }

    // Tipul de azi
    val todayWorkoutType = remember(currentMicro) {
        val dayIndex = today.dayOfWeek.value - 1 // 0=Luni
        currentMicro?.workouts?.getOrNull(dayIndex) ?: WorkoutType.REST
    }

    // Numărul săptămânii în plan
    val totalWeeks = plan.mesoCycles.sumOf { it.microCycle.size }
    val currentWeekNum = remember(currentMicro) {
        plan.mesoCycles.flatMap { it.microCycle }
            .indexOfFirst { it.startDate == currentMicro?.startDate }
            .plus(1).coerceAtLeast(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── 1. Header ──
        DashboardHeader(userName = "Stefan", email = "stefanmitroi@gmail.com")

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. Card Senzor ──
        SensorCard(
            device = uiState.device,
            vitals = uiState.vitals,
            onDisconnectClick = {
                if (uiState.device.deviceId.isNotEmpty())
                    viewModel.toggleConnection(uiState.device.deviceId)
            }
        )

        // ── 3. Workout activ (dacă e cazul) ──
        if (uiState.device.isConnected) {
            Spacer(modifier = Modifier.height(12.dp))
            WorkoutControlPanel(
                isActive          = uiState.isWorkoutActive,
                vitals            = uiState.vitals,
                onStart           = { viewModel.startWorkout() },
                onStop            = { type -> viewModel.stopWorkout(type) },
                onMaximizeWorkout = onMaximizeWorkout,
                onTypeSelected    = { type -> viewModel.setWorkoutType(type) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 4. Periodizare Bompa ──
        SectionLabel("Periodizare Bompa")
        Spacer(modifier = Modifier.height(8.dp))
        PeriodizationPhaseRow(
            phaseName    = currentMeso?.phase ?: "—",
            weekNum      = currentWeekNum,
            totalWeeks   = totalWeeks,
            mesoWeeks    = currentMeso?.microCycle?.size ?: 0,
            compDate     = effectiveCompDate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 5. CNS Readiness ──
        SectionLabel("Stare CNS — Pregătire azi")
        Spacer(modifier = Modifier.height(8.dp))
        CnsReadinessCard(cnsScore = uiState.vitals.cnsScore)

        Spacer(modifier = Modifier.height(16.dp))

        // ── 6. Sarcina săptămânii (TRIMP Acut/Cronic) ──
        SectionLabel("Sarcina săptămânii (TRIMP acumulat)")
        Spacer(modifier = Modifier.height(8.dp))
        TrainingLoadRow(trimp = uiState.vitals.trimpScore.toFloat())

        Spacer(modifier = Modifier.height(16.dp))

        // ── 7. Microciclu curent ──
        currentMicro?.let { micro ->
            SectionLabel("Microciclu curent")
            Spacer(modifier = Modifier.height(8.dp))
            MicroCycleWeekCard(
                microCycle   = micro,
                phaseName    = currentMeso?.phase ?: "",
                weekNum      = currentWeekNum,
                today        = today
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 8. Antrenamentul de azi ──
        SectionLabel("Antrenamentul de azi")
        Spacer(modifier = Modifier.height(8.dp))
        TodayWorkoutCard(
            workoutType = todayWorkoutType,
            cnsScore    = uiState.vitals.cnsScore
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 9. Tip Bompa ──
        BompaTipCard(acwrValue = 1.1f) // TODO: calcul real din sesiunile salvate

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────
// COMPONENTE
// ─────────────────────────────────────

@Composable
private fun DashboardHeader(userName: String, email: String) {
    Column {
        Text(
            text = "Salut, $userName!",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = email,
            color = Color(0xFF555566),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SensorCard(
    device: DeviceState,
    vitals: AthleteVitals,
    onDisconnectClick: () -> Unit
) {
    val bgColor = if (device.isConnected) Color(0xFF0F1F0F) else CardDark
    val borderColor = if (device.isConnected)
        Color(0xFF2D5A2D) else BorderDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = device.isConnected) { onDisconnectClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon cu inel pulsant
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (device.isConnected) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FF5252))
                    )
                }
                Icon(
                    imageVector = if (device.isConnected) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (device.isConnected) Color(0xFFFF5252) else Color(0xFF444455),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (device.isConnected) {
                    Text(
                        text = "Senzor activ",
                        color = ColorGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${vitals.heartRate} BPM",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "Apasă pentru deconectare",
                        color = Color(0xFF444455),
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Așteptare conexiune...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configurează senzorul din tab-ul 'Senzori'",
                        color = Color(0xFF555566),
                        fontSize = 11.sp
                    )
                }
            }

            if (device.isConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22004D00))
                        .border(1.dp, ColorGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Live",
                        color = ColorGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Indigo.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PeriodizationPhaseRow(
    phaseName: String,
    weekNum: Int,
    totalWeeks: Int,
    mesoWeeks: Int,
    compDate: LocalDate
) {
    val phaseColor = when (phaseName.lowercase()) {
        "general"  -> ColorEND
        "specific" -> ColorSPD
        "precomp"  -> Color(0xFFA78BFA)
        "comp"     -> Color(0xFFF87171)
        "recovery" -> Color(0xFF67E8F9)
        else       -> Color.Gray
    }
    val phaseBgColor = when (phaseName.lowercase()) {
        "general"  -> Color(0xFF1D3A2A)
        "specific" -> Color(0xFF2A1F00)
        "precomp"  -> Color(0xFF1A0D2E)
        "comp"     -> Color(0xFF1A0808)
        "recovery" -> Color(0xFF0D1A1A)
        else       -> CardDark
    }

    val formatter = DateTimeFormatter.ofPattern("dd MMM")
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), compDate)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card fază curentă
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(phaseBgColor)
                .border(1.dp, phaseColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "Faza curentă",
                    color = Color(0xFF555566),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(phaseColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = phaseName.replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Săpt. $weekNum din $totalWeeks",
                    color = Color(0xFF555566),
                    fontSize = 11.sp
                )
            }
        }

        // Card competiție
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "Competiție",
                    color = Color(0xFF555566),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = compDate.format(formatter),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$daysLeft zile rămase",
                    color = Color(0xFF555566),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CnsReadinessCard(cnsScore: Int) {
    val displayScore = cnsScore.coerceIn(0, 100)
    val animatedScore by animateFloatAsState(
        targetValue = displayScore.toFloat(),
        animationSpec = tween(800),
        label = "cns"
    )

    val (scoreColor, scoreLabel) = when {
        displayScore >= 70 -> ColorGreen to "ODIHNIT"
        displayScore >= 50 -> ColorSPD to "NORMAL"
        displayScore >  0  -> Color(0xFFF87171) to "OBOSIT"
        else               -> Color(0xFF555566) to "SE CALCULEAZĂ"
    }

    val animatedColor by animateColorAsState(
        targetValue = scoreColor,
        animationSpec = tween(500),
        label = "cnsColor"
    )

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
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "CNS Readiness",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "bazat pe RMSSD live",
                    color = Color(0xFF555566),
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${displayScore.coerceAtLeast(0)}",
                    color = animatedColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )
                Text(
                    text = scoreLabel,
                    color = animatedColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bară progres
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BorderDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedScore / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(animatedColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Epuizat", "Obosit", "Normal", "Odihnit").forEach { label ->
                Text(text = label, color = Color(0xFF333344), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TrainingLoadRow(trimp: Float) {
    // Simulăm valori - în producție vin din sesiunile salvate din Room
    val acuteLoad  = trimp.coerceAtLeast(1f)
    val chronicLoad = (trimp * 0.9f).coerceAtLeast(1f)
    val acwr = if (chronicLoad > 0) (acuteLoad / chronicLoad) else 1.0f
    val acwrColor = when {
        acwr < 0.8f -> Color(0xFFF87171)
        acwr <= 1.3f -> ColorGreen
        else -> ColorSPD
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Acut
        LoadMetricCard(
            label = "Acut (7z)",
            value = "${acuteLoad.toInt()}",
            unit  = "TRIMP",
            barFraction = (acuteLoad / 300f).coerceIn(0f, 1f),
            barColor = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
        // Cronic
        LoadMetricCard(
            label = "Cronic (28z)",
            value = "${chronicLoad.toInt()}",
            unit  = "TRIMP/z avg",
            barFraction = (chronicLoad / 300f).coerceIn(0f, 1f),
            barColor = Indigo,
            modifier = Modifier.weight(1f)
        )
        // Raport AC
        LoadMetricCard(
            label = "Raport AC",
            value = "%.1f".format(acwr),
            unit  = "optim 0.8–1.3",
            barFraction = (acwr / 2f).coerceIn(0f, 1f),
            barColor = acwrColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadMetricCard(
    label: String,
    value: String,
    unit: String,
    barFraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = barFraction,
        animationSpec = tween(800),
        label = "bar"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF555566),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = unit,
            color = Color(0xFF444455),
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(BorderDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MicroCycleWeekCard(
    microCycle: MicroCycle,
    phaseName: String,
    weekNum: Int,
    today: LocalDate
) {
    val phaseColor = when (phaseName.lowercase()) {
        "general"  -> ColorEND
        "specific" -> ColorSPD
        "precomp"  -> Color(0xFFA78BFA)
        "comp"     -> Color(0xFFF87171)
        "recovery" -> Color(0xFF67E8F9)
        else       -> Color.Gray
    }
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
            text = "Săptămâna $weekNum — Faza ${phaseName.replaceFirstChar { it.uppercase() }}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            microCycle.workouts.forEachIndexed { index, workoutType ->
                val date = microCycle.startDate.plusDays(index.toLong())
                val isToday = date == today

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isToday) Color(0xFF1A1A2E) else Color(0xFF111118)
                        )
                        .border(
                            width = if (isToday) 1.dp else 0.5.dp,
                            color = if (isToday) Indigo.copy(alpha = 0.6f) else BorderDark,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayLabels[index] + if (isToday) " ←" else "",
                        color = if (isToday) Color(0xFFA5B4FC) else Color(0xFF444455),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = workoutIcon(workoutType), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = workoutLabel(workoutType),
                        color = workoutColor(workoutType),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayWorkoutCard(
    workoutType: WorkoutType,
    cnsScore: Int
) {
    val (name, duration, intensity) = workoutRecommendation(workoutType)
    val color = workoutColor(workoutType)
    val bgColor = workoutBg(workoutType)

    // Recomandare bazată pe CNS
    val cnsHint = when {
        cnsScore >= 70 -> "Sistemul nervos e odihnit. Poți face antrenament intens."
        cnsScore >= 50 -> "Stare normală. Antrenament standard recomandat."
        cnsScore > 0   -> "CNS obosit. Prioritizează recuperarea."
        else           -> "Conectează senzorul pentru analiza CNS."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Recomandat de algoritm",
            color = Color(0xFF555566),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(14.dp))

        WorkoutDetailRow(icon = "⏱", label = "Durată:", value = duration)
        Spacer(modifier = Modifier.height(8.dp))
        WorkoutDetailRow(icon = "❤️", label = "Intensitate:", value = intensity)

        Spacer(modifier = Modifier.height(12.dp))

        // Hint CNS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(
                text = cnsHint,
                color = color.copy(alpha = 0.8f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun WorkoutDetailRow(icon: String, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BorderDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 14.sp)
        }
        Text(text = label, color = Color(0xFF888899), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BompaTipCard(acwrValue: Float) {
    val (tipText, tipColor) = when {
        acwrValue < 0.8f  -> "Raportul Acut/Cronic scăzut (${acwrValue}) — risc de sub-antrenament. Crește treptat volumul." to Color(0xFF60A5FA)
        acwrValue <= 1.3f -> "Raportul Acut/Cronic (${"%.1f".format(acwrValue)}) indică o sarcină optimă. Menține volumul fără creșteri bruște." to Indigo
        else              -> "Raportul Acut/Cronic ridicat (${"%.1f".format(acwrValue)}) — risc de suprasolicitare. Prioritizează recuperarea." to Color(0xFFF87171)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E0E1A))
            .border(1.dp, tipColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "📖", fontSize = 14.sp)
        Column {
            Text(
                text = "Bompa:",
                color = Color(0xFFA5B4FC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tipText,
                color = tipColor,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
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