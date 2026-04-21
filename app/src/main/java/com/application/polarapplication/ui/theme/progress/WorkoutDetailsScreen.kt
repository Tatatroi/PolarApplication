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
import com.application.polarapplication.ui.info.InfoIconButton
import com.application.polarapplication.ui.info.MetricInfoData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF080808)
private val CardDark = Color(0xFF111116)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)

private val Zone5Color = Color(0xFFEF4444)
private val Zone4Color = Color(0xFFF97316)
private val Zone3Color = Color(0xFF4ADE80)
private val Zone2Color = Color(0xFF60A5FA)
private val Zone1Color = Color(0xFF9CA3AF)

private fun zoneColor(zone: Int) = when (zone) {
    5 -> Zone5Color
    4 -> Zone4Color
    3 -> Zone3Color
    2 -> Zone2Color
    else -> Zone1Color
}

private fun hrToZone(hr: Int, maxHr: Int): Int {
    val pct = hr.toFloat() / maxHr
    return when {
        pct >= 0.90f -> 5
        pct >= 0.80f -> 4
        pct >= 0.70f -> 3
        pct >= 0.60f -> 2
        else -> 1
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Heart rate zones",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
            )
            InfoIconButton(info = MetricInfoData.HR_ZONES_OVERVIEW, tint = Color.White.copy(alpha = 0.25f))
        }

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
// HELPERS LACTAT
// ─────────────────────────────────────────────

private fun hrToLactate(hr: Int, maxHr: Int): String {
    val pct = hr.toFloat() / maxHr
    return when {
        pct >= 0.90f -> "> 6 mmol/L"
        pct >= 0.85f -> "4–6 mmol/L"
        pct >= 0.70f -> "2–4 mmol/L"
        else -> "< 2 mmol/L"
    }
}

private fun hrToLactateColor(hr: Int, maxHr: Int): Color {
    val pct = hr.toFloat() / maxHr
    return when {
        pct >= 0.90f -> Color(0xFFEF4444)
        pct >= 0.85f -> Color(0xFFF97316)
        pct >= 0.70f -> Color(0xFFFBBF24)
        else -> Color(0xFF60A5FA)
    }
}

// ─────────────────────────────────────────────
// GRAFIC HR CARD
// ─────────────────────────────────────────────

@Composable
private fun HrChartCard(hrList: List<Int>, maxHr: Int) {
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }

    val peakHr = if (hrList.isNotEmpty()) hrList.max() else 0
    val peakLactate = hrToLactate(peakHr, maxHr)
    val peakLactateColor = hrToLactateColor(peakHr, maxHr)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(16.dp)
    ) {
        if (hrList.isNotEmpty()) {
            HrLineChart(
                hrList = hrList,
                maxHr = maxHr,
                tooltipIndex = tooltipIndex,
                onTap = { index -> tooltipIndex = index },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No heart rate data saved", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Avg / Max / Peak Lactate ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avg. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "${if (hrList.isNotEmpty()) hrList.average().toInt() else 0} bpm",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Max. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "$peakHr bpm",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Peak lactate", color = Color.Gray, fontSize = 12.sp)
                Text(
                    peakLactate,
                    color = peakLactateColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Rândul LT1 / LT2 cu info ──────────────────────────────────────
        LactateThresholdRow(maxHr = maxHr)
    }
}

// ─────────────────────────────────────────────
// LACTATE THRESHOLD ROW
// ─────────────────────────────────────────────

@Composable
private fun LactateThresholdRow(maxHr: Int) {
    val lt1Bpm = (maxHr * 0.70).toInt()
    val lt2Bpm = (maxHr * 0.85).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // LT1
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFBBF24).copy(alpha = 0.07f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LT1 · $lt1Bpm bpm",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~2 mmol/L",
                    color = Color(0xFFFBBF24).copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
            InfoIconButton(
                info = MetricInfoData.LT1,
                tint = Color(0xFFFBBF24).copy(alpha = 0.5f)
            )
        }

        // LT2
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF97316).copy(alpha = 0.07f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LT2 · $lt2Bpm bpm",
                    color = Color(0xFFF97316),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~4 mmol/L",
                    color = Color(0xFFF97316).copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
            InfoIconButton(
                info = MetricInfoData.LT2,
                tint = Color(0xFFF97316).copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// GRAFIC LINIE CUSTOM — Option C
// spiky raw data + colored gradient fill + LT lines
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

    val lt1Hr = (maxHr * 0.70).toInt()
    val lt2Hr = (maxHr * 0.85).toInt()

    val dataMin = hrList.min()
    val dataMax = hrList.max()
    val padding = ((dataMax - dataMin) * 0.15f).toInt().coerceAtLeast(5)
    val yMin = (dataMin - padding).coerceAtLeast(0)
    val yMax = dataMax + padding
    val yRange = (yMax - yMin).toFloat()

    val yLabels = List(5) { i -> yMin + ((yRange / 4f) * (4 - i)).toInt() }

    val totalSamples = hrList.size
    val secondsPerSample = 2
    val totalSeconds = totalSamples * secondsPerSample

    // Colori LT
    val lt1Color = Color(0xFFFBBF24)
    val lt2Color = Color(0xFFF97316)

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
            val chartLeft = 48.dp.toPx()
            val chartRight = size.width - 8.dp.toPx()
            val chartTop = 8.dp.toPx()
            val chartBottom = size.height - 24.dp.toPx()
            val chartWidth = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            fun hrToY(hr: Int): Float =
                chartBottom - ((hr - yMin).toFloat() / yRange) * chartHeight

            fun indexToX(index: Int): Float =
                chartLeft + (index.toFloat() / (hrList.size - 1)) * chartWidth

            val lt1Y = hrToY(lt1Hr).coerceIn(chartTop, chartBottom)
            val lt2Y = hrToY(lt2Hr).coerceIn(chartTop, chartBottom)

            // ── Zone background bands (foarte subtile) ──────────────────────
            // Recovery zone (sub LT1) — albastru
            drawRect(
                color = Color(0xFF60A5FA).copy(alpha = 0.04f),
                topLeft = Offset(chartLeft, lt1Y),
                size = Size(chartWidth, chartBottom - lt1Y)
            )
            // Aerobic zone (LT1 → LT2) — galben
            drawRect(
                color = Color(0xFFFBBF24).copy(alpha = 0.04f),
                topLeft = Offset(chartLeft, lt2Y),
                size = Size(chartWidth, lt1Y - lt2Y)
            )
            // Anaerobic zone (peste LT2) — roșu
            drawRect(
                color = Color(0xFFEF4444).copy(alpha = 0.05f),
                topLeft = Offset(chartLeft, chartTop),
                size = Size(chartWidth, lt2Y - chartTop)
            )

            // ── Grid lines ──────────────────────────────────────────────────
            yLabels.forEachIndexed { i, label ->
                val y = chartTop + (i.toFloat() / (yLabels.size - 1)) * chartHeight
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$label",
                    chartLeft - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(120, 255, 255, 255)
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // ── Etichete axa X ───────────────────────────────────────────────
            val xLabelCount = 4
            for (i in 0..xLabelCount) {
                val frac = i.toFloat() / xLabelCount
                val x = chartLeft + frac * chartWidth
                val seconds = (frac * totalSeconds).toInt()
                val label = "%02d:%02d".format(seconds / 60, seconds % 60)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(100, 255, 255, 255)
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // ── Linii LT1 / LT2 punctate ────────────────────────────────────
            val dashLength = 8.dp.toPx()
            val gapLength = 5.dp.toPx()

            // LT1 — galben punctat
            var xCursor = chartLeft
            while (xCursor < chartRight) {
                drawLine(
                    color = lt1Color.copy(alpha = 0.45f),
                    start = Offset(xCursor, lt1Y),
                    end = Offset((xCursor + dashLength).coerceAtMost(chartRight), lt1Y),
                    strokeWidth = 1.2.dp.toPx()
                )
                xCursor += dashLength + gapLength
            }

            // LT2 — portocaliu punctat
            xCursor = chartLeft
            while (xCursor < chartRight) {
                drawLine(
                    color = lt2Color.copy(alpha = 0.45f),
                    start = Offset(xCursor, lt2Y),
                    end = Offset((xCursor + dashLength).coerceAtMost(chartRight), lt2Y),
                    strokeWidth = 1.2.dp.toPx()
                )
                xCursor += dashLength + gapLength
            }

            // Label LT1
            drawContext.canvas.nativeCanvas.drawText(
                "LT1",
                chartLeft + 4.dp.toPx(),
                lt1Y - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(160, 251, 191, 36)
                    textSize = 9.dp.toPx()
                    isFakeBoldText = true
                }
            )
            // Label LT2
            drawContext.canvas.nativeCanvas.drawText(
                "LT2",
                chartLeft + 4.dp.toPx(),
                lt2Y - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(160, 249, 115, 22)
                    textSize = 9.dp.toPx()
                    isFakeBoldText = true
                }
            )

            // ── Gradient fill colorat (Option C) ────────────────────────────
            // Fill path — raw (tension = 0, spiky)
            val fillPath = Path()
            fillPath.moveTo(indexToX(0), chartBottom)
            fillPath.lineTo(indexToX(0), hrToY(hrList[0]))
            for (i in 1 until hrList.size) {
                fillPath.lineTo(indexToX(i), hrToY(hrList[i]))
            }
            fillPath.lineTo(indexToX(hrList.size - 1), chartBottom)
            fillPath.close()

            // Gradient: roșu sus → portocaliu → galben → albastru jos
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFEF4444).copy(alpha = 0.30f),
                        0.35f to Color(0xFFF97316).copy(alpha = 0.20f),
                        0.65f to Color(0xFFFBBF24).copy(alpha = 0.12f),
                        1.0f to Color(0xFF60A5FA).copy(alpha = 0.04f)
                    ),
                    startY = chartTop,
                    endY = chartBottom
                )
            )

            // ── Linia HR — raw spiky, albă ───────────────────────────────────
            val linePath = Path()
            linePath.moveTo(indexToX(0), hrToY(hrList[0]))
            for (i in 1 until hrList.size) {
                linePath.lineTo(indexToX(i), hrToY(hrList[i]))
            }
            drawPath(
                path = linePath,
                color = Color.White.copy(alpha = 0.65f),
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // ── Tooltip la tap ───────────────────────────────────────────────
            tooltipIndex?.let { idx ->
                val x = indexToX(idx)
                val y = hrToY(hrList[idx])
                val hr = hrList[idx]
                val pct = hr.toFloat() / maxHr

                val tooltipZoneColor = when {
                    pct >= 0.90f -> android.graphics.Color.argb(255, 239, 68, 68)
                    pct >= 0.85f -> android.graphics.Color.argb(255, 249, 115, 22)
                    pct >= 0.70f -> android.graphics.Color.argb(255, 251, 191, 36)
                    else -> android.graphics.Color.argb(255, 96, 165, 250)
                }

                val lactateZone = when {
                    pct >= 0.90f -> "> 6 mmol/L"
                    pct >= 0.85f -> "~4–6 mmol/L · LT2"
                    pct >= 0.70f -> "~2–4 mmol/L · LT1"
                    else -> "< 2 mmol/L · Recovery"
                }

                val seconds = idx * secondsPerSample
                val line1 = "$hr bpm  ${"%02d:%02d".format(seconds / 60, seconds % 60)}"
                val line2 = lactateZone

                val paint1 = android.graphics.Paint().apply {
                    textSize = 11.dp.toPx()
                    color = android.graphics.Color.WHITE
                    isFakeBoldText = true
                }
                val paint2 = android.graphics.Paint().apply {
                    textSize = 10.dp.toPx()
                    color = tooltipZoneColor
                }

                val w1 = paint1.measureText(line1)
                val w2 = paint2.measureText(line2)
                val boxW = maxOf(w1, w2) + 20.dp.toPx()
                val boxH = 11.dp.toPx() + 10.dp.toPx() + 12.dp.toPx() + 6.dp.toPx()
                var boxLeft = x - boxW / 2f
                boxLeft = boxLeft.coerceIn(chartLeft, chartRight - boxW)
                val boxTop = (y - boxH - 12.dp.toPx()).coerceAtLeast(chartTop)

                // Linie verticală
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 1f
                )
                // Punct pe linie
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(
                    color = when {
                        pct >= 0.90f -> Color(0xFFEF4444)
                        pct >= 0.85f -> Color(0xFFF97316)
                        pct >= 0.70f -> Color(0xFFFBBF24)
                        else -> Color(0xFF60A5FA)
                    },
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )

                // Fundal tooltip
                drawRoundRect(
                    color = Color(0xF0111116),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 1f)
                )

                // Text linia 1 — BPM + timp
                drawContext.canvas.nativeCanvas.drawText(
                    line1,
                    boxLeft + 10.dp.toPx(),
                    boxTop + 6.dp.toPx() + 11.dp.toPx(),
                    paint1
                )
                // Text linia 2 — zona lactat
                drawContext.canvas.nativeCanvas.drawText(
                    line2,
                    boxLeft + 10.dp.toPx(),
                    boxTop + 6.dp.toPx() + 11.dp.toPx() + 12.dp.toPx(),
                    paint2
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
    val totalSamples = hrList.size.toFloat()

    val z5 = hrList.count { hrToZone(it, maxHr) == 5 }
    val z4 = hrList.count { hrToZone(it, maxHr) == 4 }
    val z3 = hrList.count { hrToZone(it, maxHr) == 3 }
    val z2 = hrList.count { hrToZone(it, maxHr) == 2 }
    val z1 = hrList.count { hrToZone(it, maxHr) == 1 }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ZoneItem(
            title = "Zone 5: Maximum",
            rangeText = "${(maxHr * 0.9).toInt()}–$maxHr bpm",
            count = z5,
            totalCount = totalSamples,
            secondsPerSample = secondsPerSample,
            color = Zone5Color,
            zoneNumber = 5
        )
        ZoneDivider()
        ZoneItem(
            title = "Zone 4: Anaerobic",
            rangeText = "${(maxHr * 0.8).toInt()}–${(maxHr * 0.9 - 1).toInt()} bpm",
            count = z4,
            totalCount = totalSamples,
            secondsPerSample = secondsPerSample,
            color = Zone4Color,
            zoneNumber = 4
        )
        ZoneDivider()
        ZoneItem(
            title = "Zone 3: Aerobic",
            rangeText = "${(maxHr * 0.7).toInt()}–${(maxHr * 0.8 - 1).toInt()} bpm",
            count = z3,
            totalCount = totalSamples,
            secondsPerSample = secondsPerSample,
            color = Zone3Color,
            zoneNumber = 3
        )
        ZoneDivider()
        ZoneItem(
            title = "Zone 2: Weight control",
            rangeText = "${(maxHr * 0.6).toInt()}–${(maxHr * 0.7 - 1).toInt()} bpm",
            count = z2,
            totalCount = totalSamples,
            secondsPerSample = secondsPerSample,
            color = Zone2Color,
            zoneNumber = 2
        )
        ZoneDivider()
        ZoneItem(
            title = "Zone 1: Low intensity",
            rangeText = "sub ${(maxHr * 0.6).toInt()} bpm",
            count = z1,
            totalCount = totalSamples,
            secondsPerSample = secondsPerSample,
            color = Zone1Color,
            zoneNumber = 1
        )
    }
}

@Composable
private fun ZoneDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.05f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 2.dp)
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
    color: Color,
    zoneNumber: Int = 0
) {
    val percentage = if (totalCount > 0) count / totalCount else 0f
    val totalSeconds = count * secondsPerSample
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val zoneInfo = when (zoneNumber) {
        5 -> MetricInfoData.ZONE_5
        4 -> MetricInfoData.ZONE_4
        3 -> MetricInfoData.ZONE_3
        2 -> MetricInfoData.ZONE_2
        1 -> MetricInfoData.ZONE_1
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                zoneInfo?.let {
                    InfoIconButton(info = it, tint = Color.White.copy(alpha = 0.25f))
                }
            }
            Text(rangeText, color = Color.Gray, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                timeString,
                color = Color.Gray,
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
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
