package com.application.polarapplication.ui.theme.progress

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

private val BgDark = Color(0xFF080808)
private val CardDark = Color(0xFF111116)
private val Zone5Color = Color(0xFFEF4444)
private val Zone4Color = Color(0xFFF97316)
private val Zone3Color = Color(0xFF4ADE80)
private val Zone2Color = Color(0xFF60A5FA)
private val Zone1Color = Color(0xFF9CA3AF)

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

@Composable
fun WorkoutDetailsScreen(session: TrainingSessionEntity, maxHr: Int = 200) {
    val hrList: List<Int> = remember(session.hrSamples) {
        try {
            val type = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(session.hrSamples, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    val totalDurationSeconds = if (session.durationSeconds > 0) {
        session.durationSeconds
    } else {
        hrList.size * 5L
    }

    val secondsPerSample = if (hrList.isNotEmpty()) {
        (totalDurationSeconds.toFloat() / hrList.size).toLong().coerceAtLeast(1L)
    } else {
        5L
    }

    val durationStr = "%02d:%02d".format(totalDurationSeconds / 60, totalDurationSeconds % 60)
    val dateStr = remember(session.date) {
        java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(session.date))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(session.type.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        ) {
            Text(dateStr, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Text(durationStr, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Text("${session.totalCalories} kcal", color = Color(0xFFFBBF24).copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Heart rate zones", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            InfoIconButton(info = MetricInfoData.HR_ZONES_OVERVIEW, tint = Color.White.copy(alpha = 0.25f))
        }

        HrChartCard(hrList = hrList, maxHr = maxHr, totalDurationSeconds = totalDurationSeconds, secondsPerSample = secondsPerSample)

        Spacer(modifier = Modifier.height(24.dp))

        if (hrList.isNotEmpty()) {
            ZoneBreakdownSection(hrList = hrList, maxHr = maxHr, secondsPerSample = secondsPerSample)
        }

        Spacer(modifier = Modifier.height(24.dp))
        AthleticImpactSection(session = session)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun HrChartCard(
    hrList: List<Int>,
    maxHr: Int,
    totalDurationSeconds: Long,
    secondsPerSample: Long
) {
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    val peakHr = if (hrList.isNotEmpty()) hrList.max() else 0
    val peakLactate = hrToLactate(peakHr, maxHr)
    val peakLactateColor = hrToLactateColor(peakHr, maxHr)

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(CardDark).padding(16.dp)
    ) {
        if (hrList.isNotEmpty()) {
            HrLineChart(
                hrList = hrList,
                maxHr = maxHr,
                totalDurationSeconds = totalDurationSeconds,
                secondsPerSample = secondsPerSample,
                tooltipIndex = tooltipIndex,
                onTap = { tooltipIndex = it },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No heart rate data saved", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avg. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text("${if (hrList.isNotEmpty()) hrList.average().toInt() else 0} bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Max. heart rate", color = Color.Gray, fontSize = 12.sp)
                Text("$peakHr bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Peak lactate", color = Color.Gray, fontSize = 12.sp)
                Text(peakLactate, color = peakLactateColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LactateThresholdRow(maxHr = maxHr)
    }
}

@Composable
private fun LactateThresholdRow(maxHr: Int) {
    val lt1Bpm = (maxHr * 0.70).toInt()
    val lt2Bpm = (maxHr * 0.85).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                Text(text = "LT1 · $lt1Bpm bpm", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "~2 mmol/L", color = Color(0xFFFBBF24).copy(alpha = 0.55f), fontSize = 11.sp)
            }
            InfoIconButton(info = MetricInfoData.LT1, tint = Color(0xFFFBBF24).copy(alpha = 0.5f))
        }

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
                Text(text = "LT2 · $lt2Bpm bpm", color = Color(0xFFF97316), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "~4 mmol/L", color = Color(0xFFF97316).copy(alpha = 0.55f), fontSize = 11.sp)
            }
            InfoIconButton(info = MetricInfoData.LT2, tint = Color(0xFFF97316).copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun HrLineChart(
    hrList: List<Int>,
    maxHr: Int,
    totalDurationSeconds: Long,
    secondsPerSample: Long,
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
    val lt1Color = Color(0xFFFBBF24)
    val lt2Color = Color(0xFFF97316)
    val showLt1 = lt1Hr in yMin..yMax
    val showLt2 = lt2Hr in yMin..yMax

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(hrList) {
                detectTapGestures { offset ->
                    val chartLeft = 48.dp.toPx()
                    val chartWidth = size.width - chartLeft - 8.dp.toPx()
                    val index = ((offset.x - chartLeft) / chartWidth * hrList.size).toInt().coerceIn(0, hrList.size - 1)
                    onTap(index)
                }
            }
        ) {
            val chartLeft = 48.dp.toPx(); val chartRight = size.width - 8.dp.toPx()
            val chartTop = 8.dp.toPx(); val chartBottom = size.height - 24.dp.toPx()
            val chartWidth = chartRight - chartLeft; val chartHeight = chartBottom - chartTop

            fun hrToY(hr: Int): Float = chartBottom - ((hr - yMin).toFloat() / yRange) * chartHeight
            fun indexToX(index: Int): Float = chartLeft + (index.toFloat() / (hrList.size - 1)) * chartWidth

            val lt1Y = hrToY(lt1Hr).coerceIn(chartTop, chartBottom)
            val lt2Y = hrToY(lt2Hr).coerceIn(chartTop, chartBottom)

            if (showLt1) drawRect(color = Color(0xFF60A5FA).copy(alpha = 0.04f), topLeft = Offset(chartLeft, lt1Y), size = Size(chartWidth, chartBottom - lt1Y))
            if (showLt1 && showLt2) drawRect(color = Color(0xFFFBBF24).copy(alpha = 0.04f), topLeft = Offset(chartLeft, lt2Y), size = Size(chartWidth, lt1Y - lt2Y))
            if (showLt2) drawRect(color = Color(0xFFEF4444).copy(alpha = 0.05f), topLeft = Offset(chartLeft, chartTop), size = Size(chartWidth, lt2Y - chartTop))

            yLabels.forEachIndexed { i, label ->
                val y = chartTop + (i.toFloat() / (yLabels.size - 1)) * chartHeight
                drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(chartLeft, y), end = Offset(chartRight, y), strokeWidth = 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    "$label",
                    chartLeft - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply { color = android.graphics.Color.argb(120, 255, 255, 255); textSize = 10.dp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
                )
            }

            val xLabelCount = 4
            for (i in 0..xLabelCount) {
                val frac = i.toFloat() / xLabelCount
                val x = chartLeft + frac * chartWidth
                val seconds = (frac * totalDurationSeconds).toLong()
                val label = "%02d:%02d".format(seconds / 60, seconds % 60)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height,
                    android.graphics.Paint().apply { color = android.graphics.Color.argb(100, 255, 255, 255); textSize = 9.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                )
            }

            val dashLength = 8.dp.toPx(); val gapLength = 5.dp.toPx()
            if (showLt1) {
                var xCursor = chartLeft
                while (xCursor < chartRight) { drawLine(color = lt1Color.copy(alpha = 0.45f), start = Offset(xCursor, lt1Y), end = Offset((xCursor + dashLength).coerceAtMost(chartRight), lt1Y), strokeWidth = 1.2.dp.toPx()); xCursor += dashLength + gapLength }
                drawContext.canvas.nativeCanvas.drawText("LT1", chartLeft + 4.dp.toPx(), lt1Y - 3.dp.toPx(), android.graphics.Paint().apply { color = android.graphics.Color.argb(160, 251, 191, 36); textSize = 9.dp.toPx(); isFakeBoldText = true })
            }
            if (showLt2) {
                var xCursor = chartLeft
                while (xCursor < chartRight) { drawLine(color = lt2Color.copy(alpha = 0.45f), start = Offset(xCursor, lt2Y), end = Offset((xCursor + dashLength).coerceAtMost(chartRight), lt2Y), strokeWidth = 1.2.dp.toPx()); xCursor += dashLength + gapLength }
                drawContext.canvas.nativeCanvas.drawText("LT2", chartLeft + 4.dp.toPx(), lt2Y - 3.dp.toPx(), android.graphics.Paint().apply { color = android.graphics.Color.argb(160, 249, 115, 22); textSize = 9.dp.toPx(); isFakeBoldText = true })
            }

            val fillPath = Path()
            fillPath.moveTo(indexToX(0), chartBottom); fillPath.lineTo(indexToX(0), hrToY(hrList[0]))
            for (i in 1 until hrList.size) fillPath.lineTo(indexToX(i), hrToY(hrList[i]))
            fillPath.lineTo(indexToX(hrList.size - 1), chartBottom); fillPath.close()
            drawPath(path = fillPath, brush = Brush.verticalGradient(colorStops = arrayOf(0.0f to Color(0xFFEF4444).copy(alpha = 0.30f), 0.35f to Color(0xFFF97316).copy(alpha = 0.20f), 0.65f to Color(0xFFFBBF24).copy(alpha = 0.12f), 1.0f to Color(0xFF60A5FA).copy(alpha = 0.04f)), startY = chartTop, endY = chartBottom))

            val linePath = Path()
            linePath.moveTo(indexToX(0), hrToY(hrList[0]))
            for (i in 1 until hrList.size) linePath.lineTo(indexToX(i), hrToY(hrList[i]))
            drawPath(path = linePath, color = Color.White.copy(alpha = 0.65f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            tooltipIndex?.let { idx ->
                val x = indexToX(idx); val y = hrToY(hrList[idx]); val hr = hrList[idx]; val pct = hr.toFloat() / maxHr
                val tooltipZoneColor = when { pct >= 0.90f -> android.graphics.Color.argb(255, 239, 68, 68); pct >= 0.85f -> android.graphics.Color.argb(255, 249, 115, 22); pct >= 0.70f -> android.graphics.Color.argb(255, 251, 191, 36); else -> android.graphics.Color.argb(255, 96, 165, 250) }
                val lactateZone = when { pct >= 0.90f -> "> 6 mmol/L"; pct >= 0.85f -> "~4–6 mmol/L · LT2"; pct >= 0.70f -> "~2–4 mmol/L · LT1"; else -> "< 2 mmol/L · Recovery" }
                val seconds = idx * secondsPerSample
                val line1 = "$hr bpm  ${"%02d:%02d".format(seconds / 60, seconds % 60)}"
                val line2 = lactateZone
                val paint1 = android.graphics.Paint().apply { textSize = 11.dp.toPx(); color = android.graphics.Color.WHITE; isFakeBoldText = true }
                val paint2 = android.graphics.Paint().apply { textSize = 10.dp.toPx(); color = tooltipZoneColor }
                val w1 = paint1.measureText(line1); val w2 = paint2.measureText(line2)
                val boxW = maxOf(w1, w2) + 20.dp.toPx(); val boxH = 11.dp.toPx() + 10.dp.toPx() + 12.dp.toPx() + 6.dp.toPx()
                var boxLeft = (x - boxW / 2f).coerceIn(chartLeft, chartRight - boxW)
                val boxTop = (y - boxH - 12.dp.toPx()).coerceAtLeast(chartTop)
                drawLine(color = Color.White.copy(alpha = 0.2f), start = Offset(x, chartTop), end = Offset(x, chartBottom), strokeWidth = 1f)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = when { pct >= 0.90f -> Color(0xFFEF4444); pct >= 0.85f -> Color(0xFFF97316); pct >= 0.70f -> Color(0xFFFBBF24); else -> Color(0xFF60A5FA) }, radius = 3.dp.toPx(), center = Offset(x, y))
                drawRoundRect(color = Color(0xF0111116), topLeft = Offset(boxLeft, boxTop), size = Size(boxW, boxH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                drawRoundRect(color = Color.White.copy(alpha = 0.08f), topLeft = Offset(boxLeft, boxTop), size = Size(boxW, boxH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()), style = Stroke(width = 1f))
                drawContext.canvas.nativeCanvas.drawText(line1, boxLeft + 10.dp.toPx(), boxTop + 6.dp.toPx() + 11.dp.toPx(), paint1)
                drawContext.canvas.nativeCanvas.drawText(line2, boxLeft + 10.dp.toPx(), boxTop + 6.dp.toPx() + 11.dp.toPx() + 12.dp.toPx(), paint2)
            }
        }
    }
}

@Composable
fun ZoneBreakdownSection(hrList: List<Int>, maxHr: Int, secondsPerSample: Long) {
    val totalSamples = hrList.size.toFloat()
    val z5 = hrList.count { hrToZone(it, maxHr) == 5 }
    val z4 = hrList.count { hrToZone(it, maxHr) == 4 }
    val z3 = hrList.count { hrToZone(it, maxHr) == 3 }
    val z2 = hrList.count { hrToZone(it, maxHr) == 2 }
    val z1 = hrList.count { hrToZone(it, maxHr) == 1 }
    Column {
        ZoneItem("Zone 5: Maximum", "${(maxHr * 0.9).toInt()}–$maxHr bpm", z5, totalSamples, secondsPerSample, Zone5Color, 5)
        ZoneDivider()
        ZoneItem("Zone 4: Anaerobic", "${(maxHr * 0.8).toInt()}–${(maxHr * 0.9 - 1).toInt()} bpm", z4, totalSamples, secondsPerSample, Zone4Color, 4)
        ZoneDivider()
        ZoneItem("Zone 3: Aerobic", "${(maxHr * 0.7).toInt()}–${(maxHr * 0.8 - 1).toInt()} bpm", z3, totalSamples, secondsPerSample, Zone3Color, 3)
        ZoneDivider()
        ZoneItem("Zone 2: Weight control", "${(maxHr * 0.6).toInt()}–${(maxHr * 0.7 - 1).toInt()} bpm", z2, totalSamples, secondsPerSample, Zone2Color, 2)
        ZoneDivider()
        ZoneItem("Zone 1: Low intensity", "sub ${(maxHr * 0.6).toInt()} bpm", z1, totalSamples, secondsPerSample, Zone1Color, 1)
    }
}

@Composable
private fun ZoneDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
}

@SuppressLint("DefaultLocale")
@Composable
private fun ZoneItem(title: String, rangeText: String, count: Int, totalCount: Float, secondsPerSample: Long, color: Color, zoneNumber: Int = 0) {
    val percentage = if (totalCount > 0) count / totalCount else 0f
    val totalSeconds = count * secondsPerSample
    val timeString = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    val zoneInfo = when (zoneNumber) { 5 -> MetricInfoData.ZONE_5; 4 -> MetricInfoData.ZONE_4; 3 -> MetricInfoData.ZONE_3; 2 -> MetricInfoData.ZONE_2; 1 -> MetricInfoData.ZONE_1; else -> null }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                zoneInfo?.let { InfoIconButton(info = it, tint = Color.White.copy(alpha = 0.25f)) }
            }
            Text(rangeText, color = Color.Gray, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(timeString, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.width(46.dp))
            Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.07f))) {
                if (percentage > 0f) Box(modifier = Modifier.fillMaxWidth(percentage.coerceAtLeast(0.01f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
            }
            Text("${(percentage * 100).toInt()}%", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun AthleticImpactSection(session: TrainingSessionEntity) {
    val intensity = when {
        session.finalTrimp > 80 || session.cnsScoreAtEnd < 40 -> "Hard"
        session.finalTrimp > 40 || session.cnsScoreAtEnd < 60 -> "Medium"
        else -> "Easy"
    }
    val pts = when (intensity) { "Hard" -> 3f; "Medium" -> 2f; else -> 1f }

    data class AxisChange(val label: String, val delta: Float, val positive: Boolean)
    val changes = mutableListOf<AxisChange>()

    when (session.type.uppercase()) {
        "STRENGTH" -> {
            changes.add(AxisChange("Strength", pts, true))
            val endBonus = when (session.activityType.lowercase()) {
                "bodyweight", "calisthenics" -> pts * 0.15f
                else -> 0f
            }
            if (endBonus > 0) changes.add(AxisChange("Endurance", endBonus, true))
            changes.add(AxisChange("HRR", if (pts >= 3f) 1.5f else 0.5f, true))
        }
        "ENDURANCE" -> {
            changes.add(AxisChange("Endurance", pts, true))
            val speedBonus = when (session.activityType.lowercase()) {
                "bag work" -> pts * 0.10f
                else -> 0f
            }
            if (speedBonus > 0) changes.add(AxisChange("Speed", speedBonus, true))
            changes.add(AxisChange("HRR", if (pts >= 3f) 1.5f else 0.5f, true))
        }
        "SPEED" -> {
            val (speedPts, strBonus, endBonus) = when (session.activityType.lowercase()) {
                "martial arts", "boxing" -> Triple(pts * 0.6f, pts * 0.2f, pts * 0.2f)
                "intervals" -> Triple(pts * 0.7f, 0f, pts * 0.3f)
                "agility" -> Triple(pts * 0.8f, pts * 0.1f, pts * 0.1f)
                else -> Triple(pts, 0f, 0f)
            }
            changes.add(AxisChange("Speed", speedPts, true))
            if (strBonus > 0) changes.add(AxisChange("Strength", strBonus, true))
            if (endBonus > 0) changes.add(AxisChange("Endurance", endBonus, true))
            changes.add(AxisChange("HRR", if (pts >= 3f) 1.5f else 0.5f, true))
        }
        "RECOVERY", "REST" -> {
            changes.add(AxisChange("HRR", 0.3f, true))
        }
    }

    // RPE bonus vizual
    val rpeNote = when {
        session.rpe in 7..8 -> " · Optimal effort"
        session.rpe >= 9 -> " · High effort"
        session.rpe in 1..3 -> " · Low effort"
        else -> ""
    }

    val activityNote = if (session.activityType.isNotEmpty()) " · ${session.activityType}" else ""
    val summaryText = "$intensity session$activityNote$rpeNote"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111118))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            "ATHLETIC IMPACT",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            changes.forEach { change ->
                val color = if (change.positive) Color(0xFF4ADE80) else Color(0xFFF87171)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        "+${"%.1f".format(change.delta)} ${change.label}",
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            summaryText,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
