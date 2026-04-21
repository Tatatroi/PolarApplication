package com.application.polarapplication.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// DATA
// ─────────────────────────────────────────────

data class MetricInfo(
    val title: String,
    val subtitle: String,
    val description: String,
    val ranges: List<Pair<String, String>> = emptyList(), // label to meaning
    val accentColor: Color = Color(0xFF818CF8)
)

object MetricInfoData {

    val TRIMP = MetricInfo(
        title = "TRIMP Score",
        subtitle = "Training Impulse",
        description = "TRIMP quantifies the total load of a training session by combining " +
            "both its intensity (heart rate zones) and its duration. " +
            "The higher the heart rate and the longer you train, the higher the TRIMP score.",
        ranges = listOf(
            "< 50" to "Light session — warm-up or recovery",
            "50–100" to "Moderate session — standard training",
            "100–150" to "Hard session — high volume or intensity",
            "> 150" to "Very hard session — race simulation or peak load"
        ),
        accentColor = Color(0xFF818CF8)
    )

    val CNS = MetricInfo(
        title = "CNS Readiness",
        subtitle = "Central Nervous System Score",
        description = "CNS Readiness reflects how recovered your nervous system is. " +
            "It is calculated from your Heart Rate Variability (HRV / RMSSD) measured live. " +
            "A high CNS score means you are ready for intense effort. " +
            "A low score suggests your body needs recovery.",
        ranges = listOf(
            "70–100" to "Rested — high-intensity training recommended",
            "50–69" to "Normal — standard training is fine",
            "1–49" to "Fatigued — prioritise recovery or light work",
            "0" to "No data — connect the sensor first"
        ),
        accentColor = Color(0xFF4ADE80)
    )

    val ZONE_5 = MetricInfo(
        title = "Zone 5 · Maximum",
        subtitle = "Above 90% of HRmax",
        description = "Maximum effort zone. Anaerobic work at near-maximal heart rate. " +
            "Used for very short sprint intervals. Cannot be sustained for more than 1–2 minutes. " +
            "Builds top-end speed and power.",
        accentColor = Color(0xFFEF4444)
    )

    val ZONE_4 = MetricInfo(
        title = "Zone 4 · Anaerobic",
        subtitle = "80–90% of HRmax",
        description = "High-intensity anaerobic zone, also known as the lactate threshold zone. " +
            "Your body produces lactic acid faster than it can clear it. " +
            "Improves lactate tolerance and race pace. Sustainable for 10–20 minutes.",
        accentColor = Color(0xFFF97316)
    )

    val ZONE_3 = MetricInfo(
        title = "Zone 3 · Aerobic",
        subtitle = "70–80% of HRmax",
        description = "Aerobic endurance zone. Moderate-to-hard effort where the body primarily " +
            "uses oxygen to burn carbohydrates. Improves aerobic capacity (VO2max). " +
            "Typical zone for tempo runs and continuous steady-state training.",
        accentColor = Color(0xFF4ADE80)
    )

    val ZONE_2 = MetricInfo(
        title = "Zone 2 · Weight Control",
        subtitle = "60–70% of HRmax",
        description = "Fat-burning and base-building zone. Your body primarily uses fat as fuel. " +
            "Essential for building aerobic base and improving fat oxidation. " +
            "Easy conversational pace — you should be able to speak in full sentences.",
        accentColor = Color(0xFF60A5FA)
    )

    val ZONE_1 = MetricInfo(
        title = "Zone 1 · Low Intensity",
        subtitle = "Below 60% of HRmax",
        description = "Very light activity — active recovery and warm-up zone. " +
            "Promotes blood flow and helps flush metabolic waste after hard sessions. " +
            "Walking, gentle cycling, or easy movement.",
        accentColor = Color(0xFF9CA3AF)
    )

    val HR_ZONES_OVERVIEW = MetricInfo(
        title = "Heart Rate Zones",
        subtitle = "5-zone training model",
        description = "Heart rate zones divide your effort into 5 levels based on your maximum " +
            "heart rate (HRmax). Each zone targets different energy systems and produces " +
            "different training adaptations. Spending the right time in each zone is the " +
            "foundation of structured training (Bompa periodisation).",
        accentColor = Color(0xFF818CF8)
    )

    val LT1 = MetricInfo(
        title = "LT1 · Aerobic Threshold",
        subtitle = "First lactate threshold · ~2 mmol/L",
        description = "LT1 is the point where your body starts producing more lactate than at rest, " +
            "but can still clear it efficiently. Below LT1 you are in a fully aerobic state — " +
            "fat is the primary fuel, breathing is comfortable, and you could sustain the effort " +
            "for hours. Training below LT1 builds aerobic base and fat-burning efficiency.\n\n" +
            "On this chart, LT1 is estimated at 70% of your maximum heart rate.",
        ranges = listOf(
            "< LT1" to "Aerobic recovery — fat burning, very sustainable",
            "At LT1" to "~2 mmol/L — transition to aerobic endurance zone",
            "LT1–LT2" to "Aerobic endurance — carbs + fat, still controlled"
        ),
        accentColor = Color(0xFFFBBF24)
    )

    val LT2 = MetricInfo(
        title = "LT2 · Anaerobic Threshold",
        subtitle = "Second lactate threshold · ~4 mmol/L",
        description = "LT2 is the critical point where lactate accumulates faster than your body " +
            "can clear it. Above LT2 you are in the anaerobic zone — muscles begin to " +
            "acidify, fatigue accelerates, and the effort becomes unsustainable within minutes. " +
            "Crossing LT2 is normal during intervals or race efforts, but sustained time " +
            "above it leads to rapid exhaustion.\n\n" +
            "On this chart, LT2 is estimated at 85% of your maximum heart rate.",
        ranges = listOf(
            "At LT2" to "~4 mmol/L — rapid lactate accumulation begins",
            "85–90%" to ">4 mmol/L — anaerobic, sustainable for 10–20 min",
            "> 90%" to ">6 mmol/L — maximum effort, 1–3 min only"
        ),
        accentColor = Color(0xFFF97316)
    )
}

// ─────────────────────────────────────────────
// DIALOG
// ─────────────────────────────────────────────

@Composable
fun MetricInfoDialog(
    info: MetricInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111118),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = info.title,
                    color = info.accentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = info.subtitle,
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.07f))
                )

                // Description
                Text(
                    text = info.description,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Ranges — only shown if provided
                if (info.ranges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reference ranges",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    info.ranges.forEach { (range, meaning) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(info.accentColor.copy(alpha = 0.06f))
                                .border(
                                    0.5.dp,
                                    info.accentColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = range,
                                color = info.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                text = meaning,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(info.accentColor.copy(alpha = 0.1f))
                    .border(1.dp, info.accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            ) {
                Text("Got it", color = info.accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ─────────────────────────────────────────────
// INFO ICON BUTTON — reusable trigger
// ─────────────────────────────────────────────

@Composable
fun InfoIconButton(
    info: MetricInfo,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.25f)
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info: ${info.title}",
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }

    if (showDialog) {
        MetricInfoDialog(
            info = info,
            onDismiss = { showDialog = false }
        )
    }
}
