package com.application.polarapplication.ui.theme.progress

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.model.TrainingSessionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.entry.entryModelOf

val AppBackground = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)
val Zone5Color = Color(0xFFD32F2F) // Roșu
val Zone4Color = Color(0xFFF57C00) // Portocaliu
val Zone3Color = Color(0xFF388E3C) // Verde
val Zone2Color = Color(0xFF1976D2) // Albastru
val Zone1Color = Color(0xFF9E9E9E) // Gri

@Composable
fun WorkoutDetailsScreen(session: TrainingSessionEntity, maxHr: Int) {
    val hrList: List<Int> = try {
        val type = object : TypeToken<List<Int>>() {}.type
        Gson().fromJson(session.hrSamples, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val chartEntryModel = if (hrList.isNotEmpty()) {
        val entries = hrList.mapIndexed { index, hr -> index to hr }.toTypedArray()
        entryModelOf(*entries)
    } else {
        entryModelOf(0 to 0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Heart rate zones",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (hrList.isNotEmpty()) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                                    lineColor = Zone4Color, // Culoarea liniei
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(Zone4Color.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                            )
                        ),
                        model = chartEntryModel,
                        startAxis = startAxis(
                            label = com.patrykandpatrick.vico.compose.component.textComponent(color = Color.Gray),
                            guideline = com.patrykandpatrick.vico.compose.component.lineComponent(color = Color.DarkGray.copy(alpha = 0.3f))
                        ),
                        bottomAxis = bottomAxis(
                            label = com.patrykandpatrick.vico.compose.component.textComponent(color = Color.Gray),
                            guideline = null
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Nu există date de puls salvate", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Avg. heart rate", color = Color.Gray, fontSize = 12.sp)
                        Text("${session.avgHeartRate} bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(
                        modifier = Modifier.height(40.dp).width(1.dp),
                        thickness = DividerDefaults.Thickness,
                        color = Color.DarkGray
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Max. heart rate", color = Color.Gray, fontSize = 12.sp)
                        Text("${session.maxHeartRate} bpm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (hrList.isNotEmpty()) {
            ZoneBreakdownSection(hrList = hrList, maxHr = maxHr)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ZoneBreakdownSection(hrList: List<Int>, maxHr: Int) {
    val z5Count = hrList.count { it >= maxHr * 0.90 }
    val z4Count = hrList.count { it >= maxHr * 0.80 && it < maxHr * 0.90 }
    val z3Count = hrList.count { it >= maxHr * 0.70 && it < maxHr * 0.80 }
    val z2Count = hrList.count { it >= maxHr * 0.60 && it < maxHr * 0.70 }
    val z1Count = hrList.count { it < maxHr * 0.60 }

    val totalSamples = hrList.size.toFloat()
    if (totalSamples == 0f) return

    val secondsPerSample = 2

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ZoneItem("Zone 5: Maximum", z5Count, totalSamples, secondsPerSample, Zone5Color, "${(maxHr * 0.9).toInt()}-$maxHr bpm")
        ZoneItem("Zone 4: Anaerobic", z4Count, totalSamples, secondsPerSample, Zone4Color, "${(maxHr * 0.8).toInt()}-${(maxHr * 0.9 - 1).toInt()} bpm")
        ZoneItem("Zone 3: Aerobic", z3Count, totalSamples, secondsPerSample, Zone3Color, "${(maxHr * 0.7).toInt()}-${(maxHr * 0.8 - 1).toInt()} bpm")
        ZoneItem("Zone 2: Weight control", z2Count, totalSamples, secondsPerSample, Zone2Color, "${(maxHr * 0.6).toInt()}-${(maxHr * 0.7 - 1).toInt()} bpm")
        ZoneItem("Zone 1: Low intensity", z1Count, totalSamples, secondsPerSample, Zone1Color, "Sub ${(maxHr * 0.6).toInt()} bpm")
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun ZoneItem(
    title: String,
    count: Int,
    totalCount: Float,
    secondsPerSample: Int,
    color: Color,
    rangeText: String
) {
    val percentage = if (totalCount > 0) (count / totalCount) else 0f
    val totalSeconds = count * secondsPerSample
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(rangeText, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(timeString, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.width(48.dp))
            Text("${(percentage * 100).toInt()}%", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.width(48.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CardSurfaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    }
}
