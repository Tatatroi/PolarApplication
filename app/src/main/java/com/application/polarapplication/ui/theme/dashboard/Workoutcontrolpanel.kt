package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ai.model.AthleteVitals

private val BgCard     = Color(0xFF111118)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentRed    = Color(0xFFF87171)
private val AccentAmber  = Color(0xFFFBBF24)

private fun typeColor(type: String) = when (type) {
    "STRENGTH"  -> AccentIndigo
    "ENDURANCE" -> AccentGreen
    "SPEED"     -> AccentAmber
    "RECOVERY"  -> Color(0xFF60A5FA)
    else        -> Color(0xFF666677)
}

@Composable
fun WorkoutControlPanel(
    isActive: Boolean,
    vitals: AthleteVitals,
    onStart: () -> Unit,
    onStop: (String) -> Unit,
    onMaximizeWorkout: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    val workoutTypes = listOf("STRENGTH", "ENDURANCE", "SPEED", "RECOVERY", "REST")
    var selectedType by remember { mutableStateOf(workoutTypes[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(
                1.dp,
                if (isActive) AccentAmber.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = if (isActive) "ACTIVE SESSION" else "PREPARE WORKOUT",
            color = if (isActive) AccentAmber else Color.White.copy(alpha = 0.25f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (!isActive) {
            // ── Recomandare CNS ──────────────────────────────────────────────
            val (recText, recColor) = when {
                vitals.cnsScore >= 80 -> "CNS rested — recommended: STRENGTH / SPEED" to AccentGreen
                vitals.cnsScore >= 50 -> "CNS normal — recommended: ENDURANCE" to AccentAmber
                vitals.cnsScore > 0   -> "CNS fatigued — recommended: RECOVERY" to AccentRed
                else                  -> "Connect sensor for CNS analysis" to Color.White.copy(alpha = 0.3f)
            }
            Text(recText, color = recColor, fontSize = 12.sp, lineHeight = 17.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Select session type:",
                color    = Color.White.copy(alpha = 0.25f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Chips tip antrenament ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                workoutTypes.forEach { type ->
                    val isSelected = selectedType == type
                    val color      = typeColor(type)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) color.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) color.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedType = type
                                onTypeSelected(type)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = type,
                            color      = if (isSelected) color else Color.White.copy(alpha = 0.35f),
                            fontSize   = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Buton Start ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(AccentIndigo.copy(alpha = 0.15f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "START WORKOUT",
                    color      = AccentIndigo,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

        } else {
            // ── Metrici live ─────────────────────────────────────────────────
            Text(
                "$selectedType · Live",
                color      = typeColor(selectedType),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("TRIMP", "%.1f".format(vitals.trimpScore), AccentIndigo),
                    Triple("CALORIES", "${vitals.calories}", AccentAmber),
                    Triple("CNS", "${vitals.cnsScore}", AccentGreen)
                ).forEach { (label, value, color) ->
                    Column(
                        modifier            = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.07f))
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buton Live details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentIndigo.copy(alpha = 0.1f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable { onMaximizeWorkout() }
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Live Details →",
                    color      = AccentIndigo,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buton Stop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = 0.1f))
                    .border(1.dp, AccentRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable { onStop(selectedType) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "STOP & SAVE",
                    color      = AccentRed,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}