package com.application.polarapplication.ai.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.model.TrainingSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// DATA CLASS
// ─────────────────────────────────────────────

data class SessionForecast(
    val predictedTrimp:    Float,
    val historicalAvg:     Float,
    val sessionCount:      Int,
    val daysSinceLast:     Int,
    val recoveryFactor:    Float,
    val cnsFactor:         Float,
    val bompaPhaseFactor:  Float,
    val conditionLabel:    String,
    val conditionColor:    Color,
    val insight:           String,
    // ── NOU ───────────────────────────────────
    val lastSessionCns:    Int,     // CNS final din ultima sesiune de același tip
    val physicalStress:    String,  // "LOW" / "MODERATE" / "HIGH" / "VERY HIGH"
    val physicalStressColor: Color
)

// ─────────────────────────────────────────────
// FORECAST ENGINE
// ─────────────────────────────────────────────

fun computeForecast(
    sessions:     List<TrainingSessionEntity>,
    workoutType:  String,
    currentPhase: String = "general"
): SessionForecast? {

    // Filtrăm sesiunile de același tip cu TRIMP valid
    val sameSessions = sessions
        .filter { it.type.uppercase() == workoutType.uppercase() && it.finalTrimp > 0 }
        .sortedByDescending { it.date }

    if (sameSessions.isEmpty()) return null

    // ── 1. CNS din ultima sesiune de ACELAȘI tip — stabil, nu live ────────────
    val lastSessionCns = sameSessions.first().cnsScoreAtEnd

    // ── 2. EWMA — media ponderată exponențial ─────────────────────────────────
    // Sesiunile mai recente au mai multă greutate
    // Dacă avem doar 1-2 sesiuni, folosim media simplă
    val historicalAvg = if (sameSessions.size == 1) {
        sameSessions.first().finalTrimp.toFloat()
    } else {
        val alpha = 0.35f
        var ewma = sameSessions.last().finalTrimp.toFloat()
        for (session in sameSessions.reversed()) {
            ewma = alpha * session.finalTrimp.toFloat() + (1f - alpha) * ewma
        }
        ewma
    }

    // ── 3. Recovery factor — zile de la ultima sesiune de ORICE tip ──────────
    val lastAnySession = sessions.maxByOrNull { it.date }
    val daysSinceLast = if (lastAnySession != null) {
        val lastDate = Instant.ofEpochMilli(lastAnySession.date)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.DAYS.between(lastDate, LocalDate.now()).toInt().coerceAtLeast(0)
    } else 0

    val recoveryFactor = when {
        daysSinceLast >= 14 -> 0.65f  // pauză lungă — corp neadaptat
        daysSinceLast >= 7  -> 0.80f  // recuperat complet
        daysSinceLast >= 3  -> 0.92f  // recuperare normală
        daysSinceLast >= 2  -> 0.97f  // aproape complet
        else                -> 1.00f  // sesiuni consecutive
    }

    // ── 4. CNS Factor — bazat pe CNS FINAL din ultima sesiune, nu live ────────
    val cnsFactor = when {
        lastSessionCns >= 80 -> 1.15f  // s-a terminat odihnit — poate mai mult azi
        lastSessionCns >= 65 -> 1.05f  // normal
        lastSessionCns >= 50 -> 0.95f  // puțin obosit la final
        lastSessionCns > 0   -> 0.80f  // obosit la finalul ultimei sesiuni
        else                 -> 1.00f  // fără date
    }

    // ── 5. Bompa Phase Factor ─────────────────────────────────────────────────
    val bompaPhaseFactor = when (currentPhase.lowercase()) {
        "general"  -> 1.00f
        "specific" -> 1.15f
        "precomp"  -> 1.10f
        "comp"     -> 0.85f
        "recovery" -> 0.60f
        else       -> 1.00f
    }

    // ── 6. Predicție TRIMP finală ─────────────────────────────────────────────
    val predicted = (historicalAvg * recoveryFactor * cnsFactor * bompaPhaseFactor)
        .coerceAtLeast(5f)

    // ── 7. Physical Stress Prediction — bazat pe TRIMP estimat ───────────────
    // TRIMP scale: 0-20 = very low, 20-50 = low, 50-80 = moderate,
    //              80-120 = high, 120+ = very high
    val (physicalStress, physicalStressColor) = when {
        predicted >= 120 -> "VERY HIGH"  to Color(0xFFEF4444)
        predicted >= 80  -> "HIGH"       to Color(0xFFF97316)
        predicted >= 50  -> "MODERATE"   to Color(0xFFFBBF24)
        predicted >= 20  -> "LOW"        to Color(0xFF4ADE80)
        else             -> "VERY LOW"   to Color(0xFF60A5FA)
    }

    // ── 8. Condition label ────────────────────────────────────────────────────
    val deltaPct = ((predicted - historicalAvg) / historicalAvg * 100).roundToInt()

    val (conditionLabel, conditionColor) = when {
        lastSessionCns in 1..49  -> "Reduce load"    to Color(0xFFF87171)
        daysSinceLast >= 14      -> "Ease back in"   to Color(0xFFFBBF24)
        deltaPct > 10            -> "Ready for more" to Color(0xFF4ADE80)
        deltaPct in -5..10       -> "Good conditions" to Color(0xFF4ADE80)
        deltaPct in -15..-6      -> "Moderate load"  to Color(0xFFFBBF24)
        else                     -> "Consider rest"  to Color(0xFFF87171)
    }

    // ── 9. Insight text ───────────────────────────────────────────────────────
    val insight = buildString {
        when {
            daysSinceLast >= 14 ->
                append("${daysSinceLast} days since last session — body has de-adapted. Start easier than usual.")
            daysSinceLast >= 7 ->
                append("${daysSinceLast} days rest — fully recovered. Good window for a quality session.")
            lastSessionCns in 1..49 ->
                append("Last session ended with low CNS (${lastSessionCns}%) — high load today may increase fatigue risk.")
            deltaPct > 15 ->
                append("Last session CNS ${lastSessionCns}% — well recovered. Conditions favor a stronger session.")
            deltaPct < -10 ->
                append("Cumulative fatigue detected. Predicted load is ${abs(deltaPct)}% below your average.")
            else ->
                append("Stable training pattern across ${sameSessions.size} ${workoutType.lowercase()} sessions.")
        }
    }

    return SessionForecast(
        predictedTrimp     = predicted,
        historicalAvg      = historicalAvg,
        sessionCount       = sameSessions.size,
        daysSinceLast      = daysSinceLast,
        recoveryFactor     = recoveryFactor,
        cnsFactor          = cnsFactor,
        bompaPhaseFactor   = bompaPhaseFactor,
        conditionLabel     = conditionLabel,
        conditionColor     = conditionColor,
        insight            = insight,
        lastSessionCns     = lastSessionCns,
        physicalStress     = physicalStress,
        physicalStressColor = physicalStressColor
    )
}

// ─────────────────────────────────────────────
// UI CARD
// ─────────────────────────────────────────────

@Composable
fun SessionForecastCard(
    sessions:     List<TrainingSessionEntity>,
    workoutType:  String,
    currentPhase: String = "general",
    modifier:     Modifier = Modifier
) {
    // ── Nu mai primim cnsScore live — îl luăm din sesiunile istorice ──────────
    val forecast = remember(sessions, workoutType, currentPhase) {
        computeForecast(sessions, workoutType, currentPhase)
    }

    if (forecast == null) return

    val accentColor  = forecast.conditionColor
    val predictedInt = forecast.predictedTrimp.roundToInt()
    val avgInt       = forecast.historicalAvg.roundToInt()
    val delta        = predictedInt - avgInt
    val deltaSign    = if (delta >= 0) "+" else ""
    val deltaPct     = if (avgInt > 0)
        ((delta.toFloat() / avgInt) * 100).roundToInt() else 0

    // Bare comparație
    val maxVal       = maxOf(forecast.predictedTrimp, forecast.historicalAvg) * 1.1f
    val avgFrac      = (forecast.historicalAvg / maxVal).coerceIn(0f, 1f)
    val predFrac     = (forecast.predictedTrimp / maxVal).coerceIn(0f, 1f)
    val animAvgFrac  by animateFloatAsState(avgFrac,  tween(800), label = "avg")
    val animPredFrac by animateFloatAsState(predFrac, tween(900), label = "pred")

    // Recovery dot
    val recoveryDotColor = when {
        forecast.daysSinceLast >= 7 -> Color(0xFF4ADE80)
        forecast.daysSinceLast >= 3 -> Color(0xFFFBBF24)
        else                        -> Color(0xFF60A5FA)
    }

    // CNS color — bazat pe CNS din ultima sesiune
    val cnsColor = when {
        forecast.lastSessionCns >= 70 -> Color(0xFF4ADE80)
        forecast.lastSessionCns >= 50 -> Color(0xFFFBBF24)
        forecast.lastSessionCns > 0   -> Color(0xFFF87171)
        else                          -> Color.White.copy(alpha = 0.2f)
    }

    val workoutIcon: ImageVector = when (workoutType.uppercase()) {
        "STRENGTH"  -> Icons.Default.FitnessCenter
        "ENDURANCE" -> Icons.Default.DirectionsRun
        "SPEED"     -> Icons.Default.Speed
        "RECOVERY"  -> Icons.Default.SelfImprovement
        else        -> Icons.Default.FitnessCenter
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x0AFFFFFF))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF818CF8).copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(14.dp)
    ) {
        // ── HEADER ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(workoutIcon, null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                Text(
                    "SESSION FORECAST",
                    color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
                Text(
                    "· $workoutType",
                    color = Color(0xFF818CF8).copy(alpha = 0.6f),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(recoveryDotColor))
                Text(
                    when {
                        forecast.daysSinceLast == 0 -> "today"
                        forecast.daysSinceLast == 1 -> "1d ago"
                        else -> "${forecast.daysSinceLast}d ago"
                    },
                    color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── TRIMP COMPARISON ──────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stânga — AVG
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text("$avgInt", color = Color.White.copy(alpha = 0.45f), fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                Text("AVG", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("TRIMP", color = Color.White.copy(alpha = 0.15f), fontSize = 8.sp, letterSpacing = 0.5.sp)
            }

            // Centru — bare
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column {
                    Text("previous", color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(animAvgFrac).fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.2f)))
                    }
                }
                Column {
                    Text("today estimate", color = accentColor.copy(alpha = 0.6f), fontSize = 8.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.04f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(animPredFrac).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.5f), accentColor)))
                        )
                    }
                }
                Text(
                    "$deltaSign$deltaPct% vs your average",
                    color = accentColor.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Medium
                )
            }

            // Dreapta — EST
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Text(if (delta >= 0) "▲" else "▼", color = accentColor, fontSize = 10.sp)
                Text("$predictedInt", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                Text("EST", color = accentColor.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("TRIMP", color = Color.White.copy(alpha = 0.15f), fontSize = 8.sp, letterSpacing = 0.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── PHYSICAL STRESS PREDICTION ────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(forecast.physicalStressColor.copy(alpha = 0.06f))
                .border(1.dp, forecast.physicalStressColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PREDICTED PHYSICAL STRESS",
                    color = Color.White.copy(alpha = 0.2f), fontSize = 8.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                )
                Text(
                    forecast.physicalStress,
                    color = forecast.physicalStressColor, fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
            // Mini stress bar
            Column(horizontalAlignment = Alignment.End) {
                Text("load level", color = Color.White.copy(alpha = 0.15f), fontSize = 7.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val filledBars = when (forecast.physicalStress) {
                        "VERY LOW"  -> 1
                        "LOW"       -> 2
                        "MODERATE"  -> 3
                        "HIGH"      -> 4
                        "VERY HIGH" -> 5
                        else        -> 0
                    }
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i < filledBars) forecast.physicalStressColor
                                    else Color.White.copy(alpha = 0.08f)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Separator
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── FOOTER: CNS ultima sesiune + Condition label ───────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // CNS din ultima sesiune — STABIL, nu live
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(
                        "CNS · LAST SESSION",
                        color = Color.White.copy(alpha = 0.2f), fontSize = 7.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    )
                    Text(
                        if (forecast.lastSessionCns > 0) "${forecast.lastSessionCns}%" else "—",
                        color = cnsColor, fontSize = 16.sp,
                        fontWeight = FontWeight.Black, lineHeight = 18.sp
                    )
                }
                // 4 dot scale
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val filledDots = when {
                        forecast.lastSessionCns >= 75 -> 4
                        forecast.lastSessionCns >= 55 -> 3
                        forecast.lastSessionCns >= 35 -> 2
                        forecast.lastSessionCns > 0   -> 1
                        else                          -> 0
                    }
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier.size(5.dp).clip(CircleShape)
                                .background(if (i < filledDots) cnsColor else Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }

            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.08f)))

            // Condition pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(forecast.conditionLabel, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── INSIGHT ───────────────────────────────────────────────────────────
        Text(
            forecast.insight,
            color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Based on ${forecast.sessionCount} ${workoutType.lowercase()} session${if (forecast.sessionCount != 1) "s" else ""} · EWMA weighted · CNS from last session",
            color = Color.White.copy(alpha = 0.12f), fontSize = 7.sp
        )
    }
}