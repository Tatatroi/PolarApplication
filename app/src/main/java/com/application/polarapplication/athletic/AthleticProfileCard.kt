package com.application.polarapplication.athletic

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val GlassBg     = Color(0xFF111118)
private val GlassBorder = Color(0x17FFFFFF)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentRed   = Color(0xFFF87171)

private fun scoreColor(score: Float) = when {
    score >= 75f -> AccentGreen
    score >= 50f -> AccentAmber
    score >= 25f -> AccentRed.copy(alpha = 0.8f)
    else         -> Color.White.copy(alpha = 0.2f)
}

// Ordinea fixă a axelor — folosită consistent peste tot
// Index: 0=Speed, 1=Strength, 2=Endurance, 3=HRR, 4=LoadBalance
private data class AxisDef(
    val label:    String,
    val abbr:     String,
    val getValue: (AthleticScore) -> Float
)

private val AXES = listOf(
    AxisDef("Speed",        "SPD") { it.speed },
    AxisDef("Strength",     "STR") { it.strength },
    AxisDef("Endurance",    "END") { it.endurance },
    AxisDef("HRR",          "HRR") { it.hrr },
    AxisDef("Load\nBalance","BAL") { it.loadBalance }
)

// Unghiuri: pornesc de la sus (-90°) în sensul acelor de ceasornic, 72° între axe
private val ANGLES = (0 until 5).map { i -> Math.toRadians(-90.0 + i * 72.0) }

// ─────────────────────────────────────────────
// PENTAGON CARD — MARE
// ─────────────────────────────────────────────

@Composable
fun AthleticProfileCardLarge(
    scores:         AthleticScore,
    modifier:       Modifier = Modifier,
    showLabels:     Boolean  = true,
    showScoreChips: Boolean  = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ATHLETIC PROFILE",
            color         = Color.White.copy(alpha = 0.25f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        PentagonCanvas(scores = scores, size = 220.dp, showLabels = showLabels)
        if (showScoreChips) {
            Spacer(modifier = Modifier.height(16.dp))
            ScoreChipsRow(scores = scores)
        }
    }
}

// ─────────────────────────────────────────────
// PENTAGON CARD — MIC
// ─────────────────────────────────────────────

@Composable
fun AthleticProfileCardSmall(
    scores:   AthleticScore,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ATHLETIC PROFILE",
            color         = Color.White.copy(alpha = 0.25f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        PentagonCanvas(scores = scores, size = 220.dp, showLabels = true)
    }
}

// ─────────────────────────────────────────────
// PENTAGON CANVAS — CORE
// ─────────────────────────────────────────────

@Composable
fun PentagonCanvas(
    scores:     AthleticScore,
    size:       Dp      = 200.dp,
    showLabels: Boolean = true
) {
    // Animăm în ACEEAȘI ordine ca AXES: Speed, Strength, Endurance, HRR, LoadBalance
    val animSpeed       by animateFloatAsState(scores.speed / 100f,
        tween(1200, 0,   FastOutSlowInEasing), label = "speed")
    val animStrength    by animateFloatAsState(scores.strength / 100f,
        tween(1200, 100, FastOutSlowInEasing), label = "strength")
    val animEndurance   by animateFloatAsState(scores.endurance / 100f,
        tween(1200, 200, FastOutSlowInEasing), label = "endurance")
    val animHrr         by animateFloatAsState(scores.hrr / 100f,
        tween(1200, 300, FastOutSlowInEasing), label = "hrr")
    val animLoadBalance by animateFloatAsState(scores.loadBalance / 100f,
        tween(1200, 400, FastOutSlowInEasing), label = "loadBalance")

    // Valorile animate în ordinea AXES
    val animValues = listOf(animSpeed, animStrength, animEndurance, animHrr, animLoadBalance)
    // Valorile raw în ordinea AXES (pentru culori și labels)
    val rawValues  = AXES.map { it.getValue(scores) }

    val maxScore = rawValues.max()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx   = size.toPx() / 2f
            val cy   = size.toPx() / 2f
            val maxR = size.toPx() / 2f * 0.75f

            // Grilă concentrică
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { level ->
                drawPolygon(Offset(cx, cy), maxR * level, ANGLES,
                    Color.White.copy(alpha = 0.05f), stroke = true, strokeW = 0.5f)
            }

            // Linii de axă
            ANGLES.forEach { angle ->
                drawLine(
                    Color.White.copy(alpha = 0.08f),
                    Offset(cx, cy),
                    Offset(cx + maxR * cos(angle).toFloat(), cy + maxR * sin(angle).toFloat()),
                    0.5f
                )
            }

            // Punctele scorurilor
            val scorePoints = animValues.mapIndexed { i, v ->
                Offset(
                    cx + (maxR * v) * cos(ANGLES[i]).toFloat(),
                    cy + (maxR * v) * sin(ANGLES[i]).toFloat()
                )
            }

            val maxIdx    = rawValues.indexOf(rawValues.max())
            val baseColor = scoreColor(rawValues[maxIdx])

            // Fill
            val path = Path()
            scorePoints.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            drawPath(path, Brush.radialGradient(
                listOf(baseColor.copy(alpha = 0.45f), baseColor.copy(alpha = 0.15f)),
                Offset(cx, cy), maxR
            ))

            // Contur
            drawPath(path, baseColor.copy(alpha = 0.8f),
                style = Stroke(1.5f, cap = StrokeCap.Round))

            // Puncte pe axe
            scorePoints.forEachIndexed { i, pt ->
                val color  = scoreColor(rawValues[i])
                val isPeak = rawValues[i] == maxScore
                if (isPeak) drawCircle(color.copy(alpha = pulseAlpha * 0.3f), 10f, pt)
                drawCircle(color, 4f, pt)
                drawCircle(Color.White.copy(alpha = 0.9f), 2f, pt)
            }
        }

        // Labels — în ordinea AXES, consistent cu canvas
        if (showLabels) {
            val labelOffsetPct = 0.92f
            AXES.forEachIndexed { i, axis ->
                val angle = ANGLES[i]
                val score = rawValues[i]
                val lx    = cos(angle) * size.value * labelOffsetPct / 2f
                val ly    = sin(angle) * size.value * labelOffsetPct / 2f

                Box(
                    modifier = Modifier
                        .size(size)
                        .wrapContentSize(unbounded = true)
                        .offset(x = lx.dp, y = ly.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            axis.label,
                            color      = scoreColor(score).copy(alpha = 0.9f),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 11.sp
                        )
                        Text(
                            "${score.toInt()}",
                            color    = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

private fun DrawScope.drawPolygon(
    center: Offset, radius: Float, angles: List<Double>,
    color: Color, stroke: Boolean = false, strokeW: Float = 1f
) {
    val path = Path()
    angles.forEachIndexed { i, angle ->
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    if (stroke) drawPath(path, color = color, style = Stroke(strokeW))
    else drawPath(path, color = color)
}

@Composable
private fun ScoreChipsRow(scores: AthleticScore) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Folosim AXES — exact aceeași ordine ca pentagon și labels
        AXES.forEach { axis ->
            val score = axis.getValue(scores)
            val color = scoreColor(score)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.08f))
                    .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    axis.abbr,
                    color         = color,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "${score.toInt()}",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    val color = scoreColor(score)
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Box(
            modifier = Modifier.weight(1f).height(4.dp)
                .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(score / 100f).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
        Text("${score.toInt()}", color = color, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
    }
}