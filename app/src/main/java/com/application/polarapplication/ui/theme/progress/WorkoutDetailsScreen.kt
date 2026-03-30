package com.application.polarapplication.ui.theme.progress

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.model.TrainingSessionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark        = Color(0xFF080808)
private val CardDark      = Color(0xFF111116)
private val GlassBg       = Color(0x0AFFFFFF)
private val GlassBorder   = Color(0x14FFFFFF)

private val Zone5Color = Color(0xFFEF4444)
private val Zone4Color = Color(0xFFF97316)
private val Zone3Color = Color(0xFF4ADE80)
private val Zone2Color = Color(0xFF60A5FA)
private val Zone1Color = Color(0xFF9CA3AF)

private fun zoneColor(zone: Int) = when (zone) {
    5    -> Zone5Color
    4    -> Zone4Color
    3    -> Zone3Color
    2    -> Zone2Color
    else -> Zone1Color
}

private fun hrToZone(hr: Int, maxHr: Int): Int {
    val pct = hr.toFloat() / maxHr
    return when {
        pct >= 0.90f -> 5
        pct >= 0.80f -> 4
        pct >= 0.70f -> 3
        pct >= 0.60f -> 2
        else         -> 1
    }
}

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun WorkoutDetailsScreen(
    session: TrainingSessionEntity,
    maxHr: Int = 200
) {
    val hrList: List<Int> = remember(session.hrSamples) {
        try {
            val type = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(session.hrSamples, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text       = "Heart rate zones",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 20.dp, top = 8.dp)
        )

        // ── Grafic HR ──────────────────────────────────────────────────────
        HrChartCard(hrList = hrList, maxHr = maxHr)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Zone breakdown ─────────────────────────────────────────────────
        if (hrList.isNotEmpty()) {
            ZoneBreakdownSection(hrList = hrList, maxHr = maxHr)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────
// GRAFIC HR CARD
// ─────────────────────────────────────────────

@Composable
private fun HrChartCard(hrList: List<Int>, maxHr: Int) {
    // Stare pentru tooltip (tap pe grafic)
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(16.dp)
    ) {
        if (hrList.isNotEmpty()) {
            // Graficul propriu-zis
            HrLineChart(
                hrList       = hrList,
                maxHr        = maxHr,
                tooltipIndex = tooltipIndex,
                onTap        = { index -> tooltipIndex = index },
                modifier     = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            Box(
                modifier         = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nu există date de puls salvate", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avg / Max HR
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avg. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "${if (hrList.isNotEmpty()) hrList.average().toInt() else 0} bpm",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(Color.DarkGray)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Max. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "${if (hrList.isNotEmpty()) hrList.max() else 0} bpm",
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// GRAFIC LINIE CUSTOM — ca Samsung Health
// ─────────────────────────────────────────────

@Composable
private fun HrLineChart(
    hrList: List<Int>,
    maxHr: Int,
    tooltipIndex: Int?,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hrList.isEmpty()) return

    // Calculăm min/max cu padding ca să nu fie flat
    val dataMin  = hrList.min()
    val dataMax  = hrList.max()
    val padding  = ((dataMax - dataMin) * 0.15f).toInt().coerceAtLeast(5)
    val yMin     = (dataMin - padding).coerceAtLeast(0)
    val yMax     = dataMax + padding
    val yRange   = (yMax - yMin).toFloat()

    // Calculăm culoarea dominantă (zona cea mai frecventă)
    val dominantZone = hrList.groupBy { hrToZone(it, maxHr) }
        .maxByOrNull { it.value.size }?.key ?: 3
    val lineColor = zoneColor(dominantZone)

    // Etichete axa Y — 5 linii grid
    val yLabels = List(5) { i ->
        yMin + ((yRange / 4f) * (4 - i)).toInt()
    }

    // Câte etichete pe X
    val totalSamples = hrList.size
    val secondsPerSample = 2
    val totalSeconds = totalSamples * secondsPerSample

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hrList) {
                    detectTapGestures { offset ->
                        val chartLeft = 48.dp.toPx()
                        val chartWidth = size.width - chartLeft - 8.dp.toPx()
                        val index = ((offset.x - chartLeft) / chartWidth * hrList.size)
                            .toInt()
                            .coerceIn(0, hrList.size - 1)
                        onTap(index)
                    }
                }
        ) {
            val chartLeft   = 48.dp.toPx()
            val chartRight  = size.width - 8.dp.toPx()
            val chartTop    = 8.dp.toPx()
            val chartBottom = size.height - 24.dp.toPx()
            val chartWidth  = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            // ── Grid lines ──────────────────────────────────────────────────
            yLabels.forEachIndexed { i, label ->
                val y = chartTop + (i.toFloat() / (yLabels.size - 1)) * chartHeight

                // Linie grid
                drawLine(
                    color       = Color.White.copy(alpha = 0.05f),
                    start       = Offset(chartLeft, y),
                    end         = Offset(chartRight, y),
                    strokeWidth = 1f
                )

                // Eticheta Y
                drawContext.canvas.nativeCanvas.drawText(
                    "$label",
                    chartLeft - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color     = android.graphics.Color.argb(120, 255, 255, 255)
                        textSize  = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // ── Etichete axa X (timp) ────────────────────────────────────────
            val xLabelCount = 4
            for (i in 0..xLabelCount) {
                val frac    = i.toFloat() / xLabelCount
                val x       = chartLeft + frac * chartWidth
                val seconds = (frac * totalSeconds).toInt()
                val mm      = seconds / 60
                val ss      = seconds % 60
                val label   = "%02d:%02d".format(mm, ss)

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height,
                    android.graphics.Paint().apply {
                        color     = android.graphics.Color.argb(100, 255, 255, 255)
                        textSize  = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // ── Path linie HR ─────────────────────────────────────────────────
            fun hrToY(hr: Int): Float =
                chartBottom - ((hr - yMin).toFloat() / yRange) * chartHeight

            fun indexToX(index: Int): Float =
                chartLeft + (index.toFloat() / (hrList.size - 1)) * chartWidth

            // Path pentru fill (gradient sub linie)
            val fillPath = Path()
            fillPath.moveTo(indexToX(0), chartBottom)
            fillPath.lineTo(indexToX(0), hrToY(hrList[0]))

            for (i in 1 until hrList.size) {
                val x0  = indexToX(i - 1)
                val y0  = hrToY(hrList[i - 1])
                val x1  = indexToX(i)
                val y1  = hrToY(hrList[i])
                val cpX = (x0 + x1) / 2f
                fillPath.cubicTo(cpX, y0, cpX, y1, x1, y1)
            }

            fillPath.lineTo(indexToX(hrList.size - 1), chartBottom)
            fillPath.close()

            // Gradient fill sub linie
            drawPath(
                path  = fillPath,
                brush = Brush.verticalGradient(
                    colors     = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = chartTop,
                    endY   = chartBottom
                )
            )

            // Path pentru linia propriu-zisă
            val linePath = Path()
            linePath.moveTo(indexToX(0), hrToY(hrList[0]))

            for (i in 1 until hrList.size) {
                val x0  = indexToX(i - 1)
                val y0  = hrToY(hrList[i - 1])
                val x1  = indexToX(i)
                val y1  = hrToY(hrList[i])
                val cpX = (x0 + x1) / 2f
                linePath.cubicTo(cpX, y0, cpX, y1, x1, y1)
            }

            drawPath(
                path        = linePath,
                color       = lineColor,
                style       = Stroke(
                    width = 2.dp.toPx(),
                    cap   = StrokeCap.Round,
                    join  = StrokeJoin.Round
                )
            )

            // ── Tooltip la tap ───────────────────────────────────────────────
            tooltipIndex?.let { idx ->
                val x   = indexToX(idx)
                val y   = hrToY(hrList[idx])
                val hr  = hrList[idx]

                // Linie verticală
                drawLine(
                    color       = Color.White.copy(alpha = 0.3f),
                    start       = Offset(x, chartTop),
                    end         = Offset(x, chartBottom),
                    strokeWidth = 1f
                )

                // Punct pe linie
                drawCircle(
                    color  = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color  = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )

                // Tooltip box
                val seconds     = idx * secondsPerSample
                val mm          = seconds / 60
                val ss          = seconds % 60
                val tooltipText = "● $hr bpm  ${"%02d:%02d".format(mm, ss)}"
                val paint       = android.graphics.Paint().apply {
                    textSize  = 11.dp.toPx()
                    color     = android.graphics.Color.WHITE
                    isFakeBoldText = true
                }
                val textWidth   = paint.measureText(tooltipText)
                val boxPadH     = 10.dp.toPx()
                val boxPadV     = 6.dp.toPx()
                val boxW        = textWidth + boxPadH * 2
                val boxH        = 11.dp.toPx() + boxPadV * 2
                var boxLeft     = x - boxW / 2f
                boxLeft = boxLeft.coerceIn(chartLeft, chartRight - boxW)
                val boxTop      = (y - boxH - 12.dp.toPx()).coerceAtLeast(chartTop)

                // Fundal tooltip
                drawRoundRect(
                    color        = Color(0xE6111116),
                    topLeft      = Offset(boxLeft, boxTop),
                    size         = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color        = lineColor.copy(alpha = 0.4f),
                    topLeft      = Offset(boxLeft, boxTop),
                    size         = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style        = Stroke(width = 1f)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    tooltipText,
                    boxLeft + boxPadH,
                    boxTop + boxPadV + 11.dp.toPx(),
                    paint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// ZONE BREAKDOWN
// ─────────────────────────────────────────────

@Composable
fun ZoneBreakdownSection(hrList: List<Int>, maxHr: Int) {
    val secondsPerSample = 2
    val totalSamples     = hrList.size.toFloat()

    val z5 = hrList.count { hrToZone(it, maxHr) == 5 }
    val z4 = hrList.count { hrToZone(it, maxHr) == 4 }
    val z3 = hrList.count { hrToZone(it, maxHr) == 3 }
    val z2 = hrList.count { hrToZone(it, maxHr) == 2 }
    val z1 = hrList.count { hrToZone(it, maxHr) == 1 }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ZoneItem(
            title           = "Zone 5: Maximum",
            rangeText       = "${(maxHr * 0.9).toInt()}–$maxHr bpm",
            count           = z5,
            totalCount      = totalSamples,
            secondsPerSample = secondsPerSample,
            color           = Zone5Color
        )
        ZoneDivider()
        ZoneItem(
            title           = "Zone 4: Anaerobic",
            rangeText       = "${(maxHr * 0.8).toInt()}–${(maxHr * 0.9 - 1).toInt()} bpm",
            count           = z4,
            totalCount      = totalSamples,
            secondsPerSample = secondsPerSample,
            color           = Zone4Color
        )
        ZoneDivider()
        ZoneItem(
            title           = "Zone 3: Aerobic",
            rangeText       = "${(maxHr * 0.7).toInt()}–${(maxHr * 0.8 - 1).toInt()} bpm",
            count           = z3,
            totalCount      = totalSamples,
            secondsPerSample = secondsPerSample,
            color           = Zone3Color
        )
        ZoneDivider()
        ZoneItem(
            title           = "Zone 2: Weight control",
            rangeText       = "${(maxHr * 0.6).toInt()}–${(maxHr * 0.7 - 1).toInt()} bpm",
            count           = z2,
            totalCount      = totalSamples,
            secondsPerSample = secondsPerSample,
            color           = Zone2Color
        )
        ZoneDivider()
        ZoneItem(
            title           = "Zone 1: Low intensity",
            rangeText       = "sub ${(maxHr * 0.6).toInt()} bpm",
            count           = z1,
            totalCount      = totalSamples,
            secondsPerSample = secondsPerSample,
            color           = Zone1Color
        )
    }
}

@Composable
private fun ZoneDivider() {
    HorizontalDivider(
        color     = Color.White.copy(alpha = 0.05f),
        thickness = 0.5.dp,
        modifier  = Modifier.padding(vertical = 2.dp)
    )
}

@SuppressLint("DefaultLocale")
@Composable
private fun ZoneItem(
    title: String,
    rangeText: String,
    count: Int,
    totalCount: Float,
    secondsPerSample: Int,
    color: Color
) {
    val percentage   = if (totalCount > 0) count / totalCount else 0f
    val totalSeconds = count * secondsPerSample
    val minutes      = totalSeconds / 60
    val seconds      = totalSeconds % 60
    val timeString   = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(rangeText, color = Color.Gray, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                timeString,
                color    = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.width(46.dp)
            )

            // Bara de progres — exact ca Samsung Health
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                if (percentage > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }

            Text(
                "${(percentage * 100).toInt()}%",
                color    = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}