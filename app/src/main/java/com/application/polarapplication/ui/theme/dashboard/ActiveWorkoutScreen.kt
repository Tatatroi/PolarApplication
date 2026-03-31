package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ui.stressPredictor.StressBodyVisualizer

// ─────────────────────────────────────────────
// ZONE HR
// ─────────────────────────────────────────────

private data class HrZoneConfig(
    val label: String,
    val shortLabel: String,
    val color: Color,
    val bgColor: Color,
    val aiStatus: String,
    val aiShort: String,
    val aiColor: Color
)

private fun getZoneConfig(heartRate: Int, maxHr: Int): HrZoneConfig {
    val pct = heartRate.toFloat() / maxHr.toFloat()
    return when {
        pct >= 0.90f -> HrZoneConfig(
            label = "Z5 · MAXIMUM",
            shortLabel = "Z5",
            color = Color(0xFFEF4444),
            bgColor = Color(0x1AEF4444),
            aiStatus = "Stres ridicat",
            aiShort = "ALERT",
            aiColor = Color(0xFFEF4444)
        )
        pct >= 0.80f -> HrZoneConfig(
            label = "Z4 · ANAEROB",
            shortLabel = "Z4",
            color = Color(0xFFF97316),
            bgColor = Color(0x1AF97316),
            aiStatus = "Stres detectat",
            aiShort = "STRES",
            aiColor = Color(0xFFF97316)
        )
        pct >= 0.70f -> HrZoneConfig(
            label = "Z3 · AEROBIC",
            shortLabel = "Z3",
            color = Color(0xFF4ADE80),
            bgColor = Color(0x1A4ADE80),
            aiStatus = "Efort aerob",
            aiShort = "AEROB",
            aiColor = Color(0xFF4ADE80)
        )
        pct >= 0.60f -> HrZoneConfig(
            label = "Z2 · CONTROL",
            shortLabel = "Z2",
            color = Color(0xFF60A5FA),
            bgColor = Color(0x1A60A5FA),
            aiStatus = "Efort ușor",
            aiShort = "UȘOR",
            aiColor = Color(0xFF60A5FA)
        )
        else -> HrZoneConfig(
            label = "SUB Z1",
            shortLabel = "Z1",
            color = Color(0xFFA3E635),
            bgColor = Color(0x1AA3E635),
            aiStatus = "Stare calmă",
            aiShort = "CALM",
            aiColor = Color(0xFF4ADE80)
        )
    }
}

// ─────────────────────────────────────────────
// CULORI GLOBALE
// ─────────────────────────────────────────────

private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)
private val GlassSmBg = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: DashboardViewModel,
    userGender: String,
    onMinimizeClick: () -> Unit
) {
    val vitals by viewModel.athleteVitals.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val workoutType by viewModel.selectedWorkoutType.collectAsState()
    val maxHr by viewModel.userMaxHr.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }

    val zoneConfig = remember(vitals.heartRate, maxHr) {
        getZoneConfig(vitals.heartRate, maxHr)
    }

    val hrFraction = (vitals.heartRate.toFloat() / maxHr.toFloat()).coerceIn(0f, 1f)
    val stressFraction = ((hrFraction - 0.4f) / 0.5f).coerceIn(0f, 1f)

    val animatedHrFraction by animateFloatAsState(
        targetValue = hrFraction,
        animationSpec = tween(600),
        label = "hrFraction"
    )
    val animatedZoneColor by animateColorAsState(
        targetValue = zoneConfig.color,
        animationSpec = tween(500),
        label = "zoneColor"
    )
    val animatedAiColor by animateColorAsState(
        targetValue = zoneConfig.aiColor,
        animationSpec = tween(500),
        label = "aiColor"
    )
    val animatedAuraScale by animateFloatAsState(
        targetValue = 1f + stressFraction * 0.25f,
        animationSpec = tween(800),
        label = "auraScale"
    )

    if (!isWorkoutActive) {
        LaunchedEffect(Unit) { onMinimizeClick() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── TopBar ──────────────────────────────────────────────────────
            TopBar(
                workoutType = workoutType,
                vitals = vitals,
                onClose = onMinimizeClick
            )

            // ── Hero: Bara HR + Omuleț ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bara HR verticală
                HrVerticalBar(
                    heartRate = vitals.heartRate,
                    hrFraction = animatedHrFraction,
                    zoneColor = animatedZoneColor,
                    zoneConfig = zoneConfig,
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                )

                // Omuleț cu aură
                BodyVisualizerPanel(
                    userGender = userGender,
                    stressFraction = stressFraction,
                    auraScale = animatedAuraScale,
                    zoneColor = animatedZoneColor,
                    workoutType = workoutType,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // ── AI Strip ─────────────────────────────────────────────────────
            AiStatusStrip(
                aiStatus = zoneConfig.aiStatus,
                aiShort = zoneConfig.aiShort,
                aiColor = animatedAiColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
            )

            // ── Metrici ───────────────────────────────────────────────────────
            MetricsRow(
                vitals = vitals,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )

            // ── Buton Stop ────────────────────────────────────────────────────
            Button(
                onClick = { showStopDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x10EF4444)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0x33EF4444)
                )
            ) {
                Text(
                    text = "ÎNCHEIE ANTRENAMENTUL",
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // ── Dialog confirmare ────────────────────────────────────────────────────
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = { Text("Finalizare sesiune", color = Color.White) },
            text = { Text("Ești sigur că vrei să oprești monitorizarea?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopWorkout(workoutType)
                    showStopDialog = false
                    onMinimizeClick()
                }) {
                    Text("DA, OPREȘTE", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("CONTINUĂ", color = Color.White)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────

@Composable
private fun TopBar(
    workoutType: String,
    vitals: com.application.polarapplication.ai.model.AthleteVitals,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Minimizează",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "Sesiune Live",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Timer (folosim TRIMP ca proxy vizual pentru timp activ)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = workoutType,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────
// BARA HR VERTICALĂ
// ─────────────────────────────────────────────

@Composable
private fun HrVerticalBar(
    heartRate: Int,
    hrFraction: Float,
    zoneColor: Color,
    zoneConfig: HrZoneConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Eticheta zonei sus
        Text(
            text = zoneConfig.shortLabel,
            color = zoneColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        // Track-ul barei
        Box(
            modifier = Modifier
                .width(28.dp)
                .weight(1f)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(hrFraction)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                zoneColor,
                                zoneColor.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }

        // BPM jos
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$heartRate",
                color = zoneColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 26.sp
            )
            Text(
                text = "BPM",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// OMULEȚ CU AURĂ
// ─────────────────────────────────────────────

@Composable
private fun BodyVisualizerPanel(
    userGender: String,
    stressFraction: Float,
    auraScale: Float,
    zoneColor: Color,
    workoutType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Folosim direct StressBodyVisualizer cu stressScore bazat pe zona HR
        // stressFraction 0.0 = Z1 (calm), 1.0 = Z5 (maxim)
        StressBodyVisualizer(
            stressScore = stressFraction,
            userGender = userGender

        )

        // Badge tip antrenament — colț dreapta sus
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = workoutType,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// AI STATUS STRIP
// ─────────────────────────────────────────────

@Composable
private fun AiStatusStrip(
    aiStatus: String,
    aiShort: String,
    aiColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dot animat
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(aiColor)
            )
            Column {
                Text(
                    text = "AI BIOMETRIC ANALYSIS",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = aiStatus,
                    color = aiColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Badge scurt dreapta
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(aiColor.copy(alpha = 0.1f))
                .border(1.dp, aiColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = aiShort,
                color = aiColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// METRICI ROW
// ─────────────────────────────────────────────

@Composable
private fun MetricsRow(
    vitals: com.application.polarapplication.ai.model.AthleteVitals,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            label = "TRIMP",
            value = "%.1f".format(vitals.trimpScore),
            unit = "",
            barFrac = (vitals.trimpScore / 150.0).toFloat().coerceIn(0f, 1f),
            barColor = Color(0xFF818CF8),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "CALORII",
            value = "${vitals.calories}",
            unit = "kcal",
            barFrac = (vitals.calories / 500f).coerceIn(0f, 1f),
            barColor = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "CNS LIVE",
            value = "${vitals.cnsScore}",
            unit = "%",
            barFrac = (vitals.cnsScore / 100f).coerceIn(0f, 1f),
            barColor = Color(0xFF4ADE80),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    barFrac: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedFrac by animateFloatAsState(
        targetValue = barFrac,
        animationSpec = tween(800),
        label = "metricBar"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 26.sp
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                color = Color.White.copy(alpha = 0.18f),
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFrac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}

// ─────────────────────────────────────────────
// METRIC SQUARE CARD (păstrat pentru compatibilitate)
// ─────────────────────────────────────────────

@Composable
fun MetricSquareCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            if (unit.isNotEmpty()) Text(text = unit, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
