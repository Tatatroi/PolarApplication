package com.application.polarapplication.ai.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val GlassBg     = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)

/**
 * Card AI Biometric Analysis
 *
 * Afișează rezultatul modelului Random Forest antrenat pe WESAD:
 * - stressLevel: 0 = Non-Stress, 1 = Stress
 * - stressScore: probabilitatea clasei stress [0.0 - 1.0]
 * - Se actualizează DOAR la fiecare fereastră de predicție (~30s)
 *   nu la fiecare sample HR
 */
@Composable
fun AiBiometricCard(
    stressLevel:   Int,    // 0 = calm, 1 = stress
    stressScore:   Float,  // probabilitate [0..1]
    heartRate:     Int,
    cnsScore:      Int,
    rmssd:         Double,
    windowSeconds: Int = 30,
    modifier:      Modifier = Modifier
) {
    // ── Countdown până la next prediction ────────────────────────────────────
    var secondsToNext by remember { mutableStateOf(windowSeconds) }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Detectăm când stressScore se schimbă → înseamnă că a venit o predicție nouă
    LaunchedEffect(stressScore, stressLevel) {
        lastUpdateTime  = System.currentTimeMillis()
        secondsToNext   = windowSeconds
    }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val elapsed = ((System.currentTimeMillis() - lastUpdateTime) / 1000).toInt()
            secondsToNext = (windowSeconds - elapsed).coerceAtLeast(0)
        }
    }

    // ── Culori și texte bazate pe stressLevel ────────────────────────────────
    val isStress       = stressLevel == 1
    val confidence     = if (isStress) stressScore else (1f - stressScore)
    val confidencePct  = (confidence * 100).toInt().coerceIn(0, 100)

    val statusColor = when {
        isStress && confidence > 0.7f -> Color(0xFFF87171)  // Stress ridicat
        isStress && confidence > 0.5f -> Color(0xFFFBBF24)  // Stress moderat
        !isStress && confidence > 0.7f -> Color(0xFF4ADE80) // Calm clar
        else -> Color(0xFF60A5FA)                            // Incert
    }

    val statusText = when {
        isStress && confidence > 0.7f  -> "HIGH PHYSICAL STRESS"
        isStress && confidence > 0.5f  -> "MODERATE PHYSICAL STRESS"
        !isStress && confidence > 0.7f -> "LOW PHYSICAL STRESS"
        else                           -> "TRANSITIONING"
    }

    val statusDetail = when {
        isStress && confidence > 0.7f  ->
            "High physiological activation detected. HR and movement patterns indicate intense effort."
        isStress && confidence > 0.5f  ->
            "Moderate physiological activation. Body is working hard but within manageable range."
        !isStress && confidence > 0.7f ->
            "Low physiological load. Body is in a relaxed or recovery state."
        else ->
            "Mixed signals — model confidence is low. May indicate transition between effort levels."
    }

    // ── Animated confidence bar ───────────────────────────────────────────────
    val animatedConf by animateFloatAsState(
        targetValue   = confidence,
        animationSpec = tween(800),
        label         = "conf"
    )

    // ── Pulsating dot ─────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue   = 0.4f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label          = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassBg)
            .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    null,
                    tint     = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "PHYSICAL STRESS ANALYSIS",
                    color         = Color.White.copy(alpha = 0.3f),
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "· WESAD RF",
                    color    = Color.White.copy(alpha = 0.15f),
                    fontSize = 8.sp
                )
            }

            // Next prediction countdown
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = pulseAlpha))
                )
                Text(
                    "next: ${secondsToNext}s",
                    color    = Color.White.copy(alpha = 0.2f),
                    fontSize = 8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Status principal ──────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                statusText,
                color      = statusColor,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            )

            // Badge confidence
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "$confidencePct%",
                    color      = statusColor,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Confidence bar ────────────────────────────────────────────────────
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Model confidence",
                    color    = Color.White.copy(alpha = 0.2f),
                    fontSize = 9.sp
                )
                Text(
                    "$confidencePct%",
                    color    = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedConf)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(statusColor.copy(alpha = 0.5f), statusColor)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Detail text ───────────────────────────────────────────────────────
        Text(
            statusDetail,
            color      = Color.White.copy(alpha = 0.45f),
            fontSize   = 10.sp,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Mini stats row ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniStatChip("HR", "${heartRate}bpm", Color(0xFF60A5FA), Modifier.weight(1f))
            MiniStatChip("HRV", "${"%.0f".format(rmssd)}ms", Color(0xFF4ADE80), Modifier.weight(1f))
            MiniStatChip("CNS", "${cnsScore}%", Color(0xFFFBBF24), Modifier.weight(1f))
            MiniStatChip(
                "WIN",
                "${windowSeconds}s",
                Color.White.copy(alpha = 0.3f),
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Disclaimer ────────────────────────────────────────────────────────
        Text(
            "Trained on WESAD dataset · BVP + ACC features · Not a medical device",
            color    = Color.White.copy(alpha = 0.1f),
            fontSize = 7.sp
        )
    }
}

@Composable
private fun MiniStatChip(
    label:    String,
    value:    String,
    color:    Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White.copy(alpha = 0.2f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}