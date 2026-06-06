package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfoData

private val GlassBg     = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)

private fun cnsColor(cns: Int) = when {
    cns >= 70 -> Color(0xFF4ADE80)
    cns >= 50 -> Color(0xFFFBBF24)
    cns > 0   -> Color(0xFFF87171)
    else      -> Color.White.copy(alpha = 0.2f)
}

private fun cnsLabel(cns: Int) = when {
    cns >= 70 -> "RESTED"
    cns >= 50 -> "NORMAL"
    cns > 0   -> "FATIGUED"
    else      -> "N/A"
}

@Composable
fun CnsEvolutionCard(sessions: List<TrainingSessionEntity>) {
    // Luăm ultimele 10 sesiuni cu CNS > 0, ordonate cronologic
    val cnsHistory = remember(sessions) {
        sessions
            .filter { it.cnsScoreAtEnd > 0 }
            .sortedBy { it.date }
            .takeLast(10)
    }

    val lastCns   = cnsHistory.lastOrNull()?.cnsScoreAtEnd ?: 0
    val color     = cnsColor(lastCns)
    val label     = cnsLabel(lastCns)
    val animFrac  by animateFloatAsState(
        targetValue   = lastCns / 100f,
        animationSpec = tween(800),
        label         = "cnsFrac"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "TRAINING READINESS",
                        color         = Color.White.copy(alpha = 0.25f),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    InfoIconButton(info = MetricInfoData.CNS, tint = Color.White.copy(alpha = 0.15f))
                }
                Text(
                    if (cnsHistory.isEmpty()) "No sessions yet"
                    else "Based on CNS recovery · last ${cnsHistory.size} session${if (cnsHistory.size > 1) "s" else ""}",
                    color    = Color.White.copy(alpha = 0.18f),
                    fontSize = 10.sp
                )
            }
            if (lastCns > 0) {
                Row(
                    verticalAlignment     = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "$lastCns",
                        color      = color,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 30.sp
                    )
                    Text(
                        label,
                        color    = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            // ── 0 sesiuni — placeholder ───────────────────────────────────────
            cnsHistory.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Complete a workout to track your training readiness",
                        color    = Color.White.copy(alpha = 0.2f),
                        fontSize = 11.sp
                    )
                }
            }

            // ── 1 sesiune — bara simplă ───────────────────────────────────────
            cnsHistory.size == 1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animFrac)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Exhausted", "Fatigued", "Normal", "Rested").forEach { lbl ->
                        Text(lbl, color = Color.White.copy(alpha = 0.12f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── 2+ sesiuni — grafic linie ─────────────────────────────────────
            else -> {
                CnsLineChart(
                    cnsValues = cnsHistory.map { it.cnsScoreAtEnd },
                    modifier  = Modifier.fillMaxWidth().height(72.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Trend
                val first = cnsHistory.first().cnsScoreAtEnd
                val last  = cnsHistory.last().cnsScoreAtEnd
                val delta = last - first
                val trendColor = when {
                    delta > 5  -> Color(0xFF4ADE80)
                    delta < -5 -> Color(0xFFF87171)
                    else       -> Color(0xFFFBBF24)
                }
                val trendText = when {
                    delta > 5  -> "↑ Recovery improving — ready for intensity"
                    delta < -5 -> "↓ Fatigue accumulating — consider recovery day"
                    else       -> "→ Recovery stable"
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(trendColor)
                    )
                    Text(trendText, color = trendColor.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// MINI LINE CHART
// ─────────────────────────────────────────────

@Composable
private fun CnsLineChart(
    cnsValues: List<Int>,
    modifier:  Modifier = Modifier
) {
    if (cnsValues.size < 2) return

    Canvas(modifier = modifier) {
        val minV  = cnsValues.min().toFloat().coerceAtMost(40f)
        val maxV  = cnsValues.max().toFloat().coerceAtLeast(minV + 10f)
        val range = maxV - minV
        val stepX = size.width / (cnsValues.size - 1)

        fun valueToY(v: Int) = size.height - ((v - minV) / range) * size.height

        // Zone bands sottili
        val zones = listOf(
            0f  to 40f  to Color(0xFFF87171),  // Exhausted
            40f to 50f  to Color(0xFFF97316),  // Fatigued
            50f to 70f  to Color(0xFFFBBF24),  // Normal
            70f to 100f to Color(0xFF4ADE80)   // Rested
        )
        zones.forEach { (range2, color) ->
            val yTop = size.height - ((range2.second.coerceAtMost(maxV) - minV) / range) * size.height
            val yBot = size.height - ((range2.first.coerceAtLeast(minV) - minV) / range) * size.height
            if (yTop < yBot) {
                drawRect(
                    color    = color.copy(alpha = 0.04f),
                    topLeft  = Offset(0f, yTop),
                    size     = androidx.compose.ui.geometry.Size(size.width, yBot - yTop)
                )
            }
        }

        // Fill sotto la linea
        val fillPath = Path().apply {
            moveTo(0f, size.height)
            cnsValues.forEachIndexed { i, v ->
                val x = i * stepX
                val y = valueToY(v)
                if (i == 0) lineTo(x, y) else lineTo(x, y)
            }
            lineTo((cnsValues.size - 1) * stepX, size.height)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                listOf(Color(0xFF818CF8).copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Linea colorata per zona
        for (i in 1 until cnsValues.size) {
            val x1    = (i - 1) * stepX
            val y1    = valueToY(cnsValues[i - 1])
            val x2    = i * stepX
            val y2    = valueToY(cnsValues[i])
            val segColor = cnsColor(cnsValues[i])
            drawLine(
                color       = segColor,
                start       = Offset(x1, y1),
                end         = Offset(x2, y2),
                strokeWidth = 2.dp.toPx(),
                cap         = StrokeCap.Round
            )
        }

        // Punti
        cnsValues.forEachIndexed { i, v ->
            val x = i * stepX
            val y = valueToY(v)
            drawCircle(cnsColor(v), 3.dp.toPx(), Offset(x, y))
        }

        // Ultimo punto più grande
        val lastX = (cnsValues.size - 1) * stepX
        val lastY = valueToY(cnsValues.last())
        drawCircle(Color.White, 4.dp.toPx(), Offset(lastX, lastY))
        drawCircle(cnsColor(cnsValues.last()), 2.5.dp.toPx(), Offset(lastX, lastY))
    }
}