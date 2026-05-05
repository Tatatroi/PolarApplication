package com.application.polarapplication.athletic

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgDark       = Color(0xFF080808)
private val GlassBg      = Color(0xFF111118)
private val GlassBorder  = Color(0x17FFFFFF)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentRed    = Color(0xFFF87171)
private val AccentAmber  = Color(0xFFFBBF24)
private val AccentBlue   = Color(0xFF60A5FA)
private val AccentPurple = Color(0xFF818CF8)

// ─────────────────────────────────────────────
// TEST STEPS
// ─────────────────────────────────────────────

private data class TestStep(
    val index:       Int,
    val title:       String,
    val instruction: String,
    val durationSec: Int,
    val icon:        ImageVector,
    val color:       Color,
    val axisTag:     String,
    val tip:         String
)

private val TEST_STEPS = listOf(
    TestStep(
        index       = 0,
        title       = "Endurance Test",
        instruction = "Walk briskly or jog lightly. Keep a pace you can maintain without stopping. Breathe through your nose if possible.",
        durationSec = 180,
        icon        = Icons.Default.DirectionsRun,
        color       = AccentGreen,
        axisTag     = "ENDURANCE",
        tip         = "Focus on steady breathing and pace."
    ),
    TestStep(
        index       = 1,
        title       = "Recovery Rest",
        instruction = "Stand still or sit. Breathe slowly and relax completely. This measures your resting recovery.",
        durationSec = 120,
        icon        = Icons.Default.SelfImprovement,
        color       = AccentBlue,
        axisTag     = "RECOVERY",
        tip         = "Relax your body and focus on slow breaths."
    ),
    TestStep(
        index       = 2,
        title       = "Strength Test",
        instruction = "Perform continuous bodyweight squats at a steady pace — one squat every 2 seconds. Keep your back straight.",
        durationSec = 120,
        icon        = Icons.Default.FitnessCenter,
        color       = AccentPurple,
        axisTag     = "STRENGTH",
        tip         = "Focus on form and a consistent rhythm."
    ),
    TestStep(
        index       = 3,
        title       = "Recovery Pause",
        instruction = "Stand still. Do not move. This measures how quickly your heart rate drops after strength exercise.",
        durationSec = 90,
        icon        = Icons.Default.Favorite,
        color       = AccentRed,
        axisTag     = "HRR",
        tip         = "Stay completely still for accurate results."
    ),
    TestStep(
        index       = 4,
        title       = "Speed Test",
        instruction = "Perform 3 maximum-effort sprints of 20 seconds each, with 40 seconds of walking rest between them. Go as fast as you can.",
        durationSec = 180,
        icon        = Icons.Default.Speed,
        color       = AccentAmber,
        axisTag     = "SPEED",
        tip         = "Give maximum effort on each sprint."
    )
)

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@Composable
fun InitialTestScreen(
    viewModel:      DashboardViewModel = viewModel(),
    athleticMgr:    AthleticProfileManager,
    onTestComplete: () -> Unit
) {
    val vitals by viewModel.athleteVitals.collectAsState()
    val maxHr  by viewModel.userMaxHr.collectAsState()

    // -1 = intro, 0..4 = pași test, 5 = rezultate
    var currentStepIdx by remember { mutableStateOf(-1) }
    var timerRunning   by remember { mutableStateOf(false) }
    var timerRemaining by remember { mutableStateOf(0) }
    var stepComplete   by remember { mutableStateOf(false) }

    val completedSteps = remember { mutableStateListOf<Boolean>().also { list ->
        repeat(TEST_STEPS.size) { list.add(false) }
    }}

    val stepHrData     = remember { mutableStateListOf<List<Int>>() }
    val currentStepHr  = remember { mutableStateListOf<Int>() }

    // Timer LaunchedEffect
    LaunchedEffect(timerRunning, currentStepIdx) {
        if (timerRunning && currentStepIdx >= 0 && currentStepIdx < TEST_STEPS.size) {
            val step = TEST_STEPS[currentStepIdx]
            timerRemaining = step.durationSec
            currentStepHr.clear()

            while (timerRemaining > 0 && timerRunning) {
                delay(1000)
                timerRemaining--
                if (vitals.heartRate > 0) currentStepHr.add(vitals.heartRate)
            }

            if (timerRemaining == 0) {
                stepComplete = true
                timerRunning = false
                stepHrData.add(currentStepHr.toList())
                completedSteps[currentStepIdx] = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        when {
            currentStepIdx == -1 -> {
                IntroScreen(
                    isDeviceConnected = viewModel.uiState.collectAsState().value.device.isConnected,
                    completedSteps    = completedSteps,
                    onStart = {
                        currentStepIdx = 0
                        timerRunning   = false
                        stepComplete   = false
                    }
                )
            }

            currentStepIdx < TEST_STEPS.size -> {
                val step = TEST_STEPS[currentStepIdx]
                TestStepScreen(
                    step           = step,
                    totalSteps     = TEST_STEPS.size,
                    timerRemaining = timerRemaining,
                    timerRunning   = timerRunning,
                    stepComplete   = stepComplete,
                    currentHr      = vitals.heartRate,
                    maxHr          = maxHr,
                    onStart = {
                        timerRunning = true
                        stepComplete = false
                    },
                    onNext = {
                        if (currentStepIdx < TEST_STEPS.size - 1) {
                            currentStepIdx++
                            timerRunning   = false
                            stepComplete   = false
                            timerRemaining = TEST_STEPS[currentStepIdx].durationSec
                        } else {
                            currentStepIdx = TEST_STEPS.size
                        }
                    }
                )
            }

            else -> {
                ResultsScreen(
                    stepHrData  = stepHrData,
                    maxHr       = maxHr,
                    athleticMgr = athleticMgr,
                    onSave      = { onTestComplete() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// INTRO SCREEN — Timeline vertical ca în design
// ─────────────────────────────────────────────

@Composable
private fun IntroScreen(
    isDeviceConnected: Boolean,
    completedSteps:    List<Boolean>,
    onStart:           () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentIndigo.copy(alpha = 0.12f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MonitorHeart, null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Athletic Profile Test",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign  = TextAlign.Center
        )
        Text(
            "A 15-minute evaluation to build your initial athletic profile.",
            color      = Color.White.copy(alpha = 0.35f),
            fontSize   = 13.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 19.sp,
            modifier   = Modifier.padding(top = 6.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
        )

        // ── TIMELINE ─────────────────────────────────────────────────────────
        TEST_STEPS.forEachIndexed { index, step ->
            val isDone = completedSteps.getOrElse(index) { false }

            Row(
                modifier  = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Coloana stângă: linie + cerc
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.width(40.dp)
                ) {
                    // Linie de sus (nu pentru primul element)
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(16.dp)
                                .background(
                                    if (completedSteps.getOrElse(index - 1) { false })
                                        step.color.copy(alpha = 0.5f)
                                    else
                                        Color.White.copy(alpha = 0.08f)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Cercul de stare
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> step.color.copy(alpha = 0.2f)
                                    else   -> Color.White.copy(alpha = 0.04f)
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    isDone -> step.color
                                    else   -> Color.White.copy(alpha = 0.12f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint     = step.color,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                        }
                    }

                    // Linie de jos (nu pentru ultimul element)
                    if (index < TEST_STEPS.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (index == 0) 60.dp else 60.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Card-ul pasului
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDone) step.color.copy(alpha = 0.08f)
                                else GlassBg
                            )
                            .border(
                                1.dp,
                                if (isDone) step.color.copy(alpha = 0.3f) else GlassBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Icon cerc
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(step.color.copy(alpha = 0.12f))
                                .border(1.dp, step.color.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(step.icon, null, tint = step.color, modifier = Modifier.size(20.dp))
                        }

                        // Titlu + descriere
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                step.title,
                                color      = if (isDone) step.color else Color.White,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                step.instruction.take(55) + "...",
                                color      = Color.White.copy(alpha = 0.3f),
                                fontSize   = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        // Durată + icon play/check
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "%d:%02d".format(step.durationSec / 60, step.durationSec % 60),
                                color      = step.color.copy(alpha = 0.7f),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isDone) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint     = step.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                // Play dezactivat — gri
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint     = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Footer cu total durată ────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Flag, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                Text(
                    "Complete all tests to build your profile",
                    color    = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
            Text(
                "Total duration: 14:30",
                color      = Color.White.copy(alpha = 0.2f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning senzor
        if (!isDeviceConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentAmber.copy(alpha = 0.07f))
                    .border(1.dp, AccentAmber.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                Text(
                    "Connect your Polar sensor for accurate results.",
                    color    = AccentAmber.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Buton Start
        Button(
            onClick  = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.18f)),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
        ) {
            Text(
                "Start Evaluation",
                color      = AccentIndigo,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// TEST STEP SCREEN
// ─────────────────────────────────────────────

@Composable
private fun TestStepScreen(
    step:           TestStep,
    totalSteps:     Int,
    timerRemaining: Int,
    timerRunning:   Boolean,
    stepComplete:   Boolean,
    currentHr:      Int,
    maxHr:          Int,
    onStart:        () -> Unit,
    onNext:         () -> Unit
) {
    val minutes  = timerRemaining / 60
    val seconds  = timerRemaining % 60
    val total    = TEST_STEPS[step.index].durationSec
    val progress = if (!timerRunning && !stepComplete) 0f
    else if (stepComplete) 1f
    else 1f - (timerRemaining.toFloat() / total.toFloat())

    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(500),
        label         = "progress"
    )

    val hrColor = when {
        currentHr == 0                           -> Color.White.copy(alpha = 0.3f)
        currentHr.toFloat() / maxHr >= 0.9f      -> AccentRed
        currentHr.toFloat() / maxHr >= 0.7f      -> AccentAmber
        else                                     -> AccentGreen
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Progress bar top + step indicator ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalSteps) { i ->
                val filled = i < step.index || (i == step.index && stepComplete)
                val active = i == step.index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                filled -> step.color
                                active -> step.color.copy(alpha = animProgress)
                                else   -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                )
            }
        }

        Text(
            "Step ${step.index + 1} of $totalSteps",
            color         = Color.White.copy(alpha = 0.25f),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(step.color.copy(alpha = 0.12f))
                .border(1.dp, step.color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(step.icon, null, tint = step.color, modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(step.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            step.instruction,
            color      = Color.White.copy(alpha = 0.4f),
            fontSize   = 13.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 19.sp,
            modifier   = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Timer circular ────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val stroke = 8.dp.toPx()
                drawArc(
                    color      = Color.White.copy(alpha = 0.05f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (timerRunning || stepComplete) {
                    drawArc(
                        color      = if (stepComplete) AccentGreen else step.color,
                        startAngle = -90f,
                        sweepAngle = 360f * animProgress,
                        useCenter  = false,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (stepComplete) {
                    Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(38.dp))
                    Text("Done!", color = AccentGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
                } else {
                    Text("%d:%02d".format(minutes, seconds), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("remaining", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tip + HR live
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(step.color.copy(alpha = 0.07f))
                .border(1.dp, step.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Star, null, tint = step.color, modifier = Modifier.size(14.dp))
            Text(step.tip, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (currentHr > 0 && (timerRunning || stepComplete)) {
                Icon(Icons.Default.Favorite, null, tint = hrColor, modifier = Modifier.size(12.dp))
                Text("$currentHr", color = hrColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Buton ─────────────────────────────────────────────────────────────
        when {
            !timerRunning && !stepComplete -> {
                // START — activ, colorat
                Button(
                    onClick  = onStart,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = step.color.copy(alpha = 0.2f)),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, step.color.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = step.color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Test", color = step.color, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }

            stepComplete -> {
                // NEXT STEP — verde
                val isLast = step.index == TEST_STEPS.size - 1
                Button(
                    onClick  = onNext,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f)),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        if (isLast) "See Results →" else "Next Step →",
                        color      = AccentGreen,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            else -> {
                // RECORDING — dezactivat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Punct animat
                        val infiniteTransition = rememberInfiniteTransition(label = "dot")
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue  = 0.3f,
                            targetValue   = 1f,
                            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                            label         = "dotAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(step.color.copy(alpha = dotAlpha))
                        )
                        Text("Recording...", color = Color.White.copy(alpha = 0.25f), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// RESULTS SCREEN
// ─────────────────────────────────────────────

@Composable
private fun ResultsScreen(
    stepHrData:  List<List<Int>>,
    maxHr:       Int,
    athleticMgr: AthleticProfileManager,
    onSave:      () -> Unit
) {
    val scores = remember(stepHrData) {
        calculateScoresFromTestData(stepHrData, maxHr, athleticMgr)
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.12f))
                .border(1.dp, AccentGreen.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.EmojiEvents, null, tint = AccentGreen, modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Test Complete!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(
            "Your initial athletic profile has been calculated.",
            color      = Color.White.copy(alpha = 0.35f),
            fontSize   = 13.sp,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        AthleticProfileCardLarge(scores = scores, showScoreChips = true)

        Spacer(modifier = Modifier.height(16.dp))

        // Explicație scoruri
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "HOW SCORES WORK",
                color         = Color.White.copy(alpha = 0.25f),
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            listOf(
                Triple(AccentGreen,  "Endurance",  "How efficiently you maintain effort at low intensity"),
                Triple(AccentPurple, "Strength",   "Muscular endurance and cardiovascular response to effort"),
                Triple(AccentAmber,  "Speed",      "Explosive capacity and peak HR under maximum effort"),
                Triple(AccentRed,    "HRR",        "How fast your heart recovers after intense exercise")
            ).forEach { (color, label, desc) ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color).padding(top = 4.dp))
                    Column {
                        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Scores update automatically after each training session.",
            color      = Color.White.copy(alpha = 0.2f),
            fontSize   = 11.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 16.sp,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Button(
            onClick  = {
                athleticMgr.saveInitialTestResults(
                    strengthScore  = scores.strength,
                    speedScore     = scores.speed,
                    enduranceScore = scores.endurance,
                    hrrScore       = scores.hrr
                )
                onSave()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.18f)),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
        ) {
            Text("Save My Profile", color = AccentIndigo, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// CALCUL SCORURI — Calibrat pentru toți nivelele
//
// Logica:
// - Scorurile sunt relative la maxHr-ul personal al utilizatorului
// - Un atlet avansat va atinge Z4-Z5 în teste și va recupera rapid → scor mare
// - Un începător va atinge Z2-Z3 și va recupera lent → scor mai mic
// - Asta face scorurile corecte indiferent de nivel
// ─────────────────────────────────────────────

private fun calculateScoresFromTestData(
    stepHrData:  List<List<Int>>,
    maxHr:       Int,
    athleticMgr: AthleticProfileManager
): AthleticScore {

    val enduranceHr = stepHrData.getOrNull(0) ?: emptyList()
    val recoveryHr  = stepHrData.getOrNull(1) ?: emptyList()
    val strengthHr  = stepHrData.getOrNull(2) ?: emptyList()
    val hrrHr       = stepHrData.getOrNull(3) ?: emptyList()
    val speedHr     = stepHrData.getOrNull(4) ?: emptyList()

    // ── ENDURANCE ─────────────────────────────────────────────────────────────
    // Măsurăm cât timp ai menținut Z2 (60-70% maxHr) și stabilitatea HR
    // Avansații: HR stabil în Z2-Z3, fără drift mare
    // Începătorii: HR urcă mult în Z3-Z4, drift mai mare
    val z2Low  = (maxHr * 0.60).toInt()
    val z2High = (maxHr * 0.75).toInt()  // extindem puțin Z2 ca să fie fair
    val z2Samples = enduranceHr.count { it in z2Low..z2High }

    val enduranceDrift = if (enduranceHr.size >= 10) {
        val firstThird = enduranceHr.take(enduranceHr.size / 3).average().toFloat()
        val lastThird  = enduranceHr.takeLast(enduranceHr.size / 3).average().toFloat()
        (lastThird - firstThird).coerceAtLeast(0f)  // cât a urcat HR în timp
    } else 0f

    val enduranceScore = athleticMgr.calculateEnduranceScore(
        z2Samples, enduranceHr.size, enduranceDrift
    )

    // ── STRENGTH ──────────────────────────────────────────────────────────────
    // Genuflexiuni continue 2 minute
    // Avansații: HR în Z3-Z4 (70-85%), recuperare rapidă după
    // Începătorii: HR în Z4-Z5 (>85%), recuperare lentă
    val avgStrengthHr = if (strengthHr.isNotEmpty()) strengthHr.average().toInt() else 0

    // HR după 90 secunde de recuperare (nu secunda 90 din array!)
    // hrrHr e înregistrat în pasul 3 (Recovery Pause), luăm media ultimelor 30 secunde
    val hrAfter90Rest = if (hrrHr.size >= 30) {
        hrrHr.takeLast(30).average().toInt()
    } else {
        hrrHr.lastOrNull() ?: avgStrengthHr
    }

    val strengthScore = if (avgStrengthHr > 0) {
        athleticMgr.calculateStrengthScore(avgStrengthHr, hrAfter90Rest, maxHr)
    } else 20f

    // ── SPEED ─────────────────────────────────────────────────────────────────
    // 3 sprinturi maxime
    // Avansații: HR peak > 90% maxHr, recuperare rapidă între sprinturi
    // Începătorii: HR peak 75-85% maxHr, recuperare mai lentă
    val peakSpeedHr = speedHr.maxOrNull() ?: 0

    // HR după 60 secunde de la finalul testului — luăm ultima valoare din speed test
    // ca proxy pentru recuperare între sprinturi
    val hrAfter60Speed = if (speedHr.size >= 40) {
        speedHr.takeLast(20).average().toInt()  // media ultimelor 20s (recuperare după ultimul sprint)
    } else {
        speedHr.lastOrNull() ?: peakSpeedHr
    }

    val speedScore = if (peakSpeedHr > 0) {
        athleticMgr.calculateSpeedScore(peakSpeedHr, hrAfter60Speed, maxHr)
    } else 20f

    // ── HRR (Heart Rate Recovery) ─────────────────────────────────────────────
    // Cât de repede scade HR după efort intens
    // Avansații: scad 25-40+ BPM în primul minut
    // Începătorii: scad 10-20 BPM în primul minut
    val peakBeforeRest = strengthHr.maxOrNull() ?: 0

    // HR la secunda 60 din faza de recuperare
    val hrAt60SecRest = if (hrrHr.size >= 60) {
        hrrHr[59]  // secunda 60 exactă
    } else {
        hrrHr.getOrNull(hrrHr.size - 1) ?: peakBeforeRest
    }

    val hrrScore = if (peakBeforeRest > 0) {
        athleticMgr.calculateHrrScore(peakBeforeRest, hrAt60SecRest)
    } else 20f

    return AthleticScore(
        strength    = strengthScore,
        speed       = speedScore,
        endurance   = enduranceScore,
        hrr         = hrrScore,
        loadBalance = 50f
    )
}