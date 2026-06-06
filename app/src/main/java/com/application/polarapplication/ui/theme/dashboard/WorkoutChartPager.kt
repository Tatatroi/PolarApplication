package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ai.analysis.AiBiometricCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutChartPager(
    samples:      List<HrSample>,
    peaks:        List<Peak>,
    maxHr:        Int,
    workoutType:  String,
    // ── Parametri noi pentru AI slide ─────────────────────────────────────────
    stressLevel:  Int    = 0,
    stressScore:  Float  = 0f,
    heartRate:    Int    = 0,
    cnsScore:     Int    = 0,
    rmssd:        Double = 0.0,
    modifier:     Modifier = Modifier
) {
    val pageLabels = listOf("Timeline", "Zones", "Recovery", "AI Analysis")
    val pagerState = rememberPagerState(pageCount = { pageLabels.size })

    Column(modifier = modifier) {
        // ── Page label + indicators ───────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                pageLabels[pagerState.currentPage],
                color      = if (pagerState.currentPage == 3)
                    Color(0xFF818CF8) // AI slide — indigo
                else
                    Color.White.copy(alpha = 0.7f),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                pageLabels.forEachIndexed { idx, _ ->
                    val isActive = pagerState.currentPage == idx
                    val dotColor = when {
                        isActive && idx == 3 -> Color(0xFF818CF8) // AI — indigo
                        isActive             -> Color(0xFF818CF8)
                        else                 -> Color.White.copy(alpha = 0.25f)
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }

        // ── Pager ─────────────────────────────────────────────────────────────
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WorkoutTimelineChart(
                    samples  = samples,
                    peaks    = peaks,
                    maxHr    = maxHr,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> WorkoutZoneChart(
                    samples    = samples,
                    targetZone = when (workoutType.uppercase()) {
                        "ENDURANCE" -> 3
                        "SPEED"     -> 4
                        "STRENGTH"  -> 3
                        "RECOVERY"  -> 2
                        else        -> 3
                    },
                    modifier   = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                )
                2 -> WorkoutRecoveryChart(
                    samples  = samples,
                    peaks    = peaks,
                    modifier = Modifier.fillMaxSize()
                )
                3 -> AiBiometricCard(
                    stressLevel   = stressLevel,
                    stressScore   = stressScore,
                    heartRate     = heartRate,
                    cnsScore      = cnsScore,
                    rmssd         = rmssd,
                    windowSeconds = 30,
                    modifier      = Modifier.fillMaxSize()
                )
            }
        }

        // ── Swipe hint ────────────────────────────────────────────────────────
        if (pagerState.currentPage == 0 && samples.size < 10) {
            Text(
                "← swipe for zones, recovery & AI →",
                color    = Color.White.copy(alpha = 0.15f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )
        }
    }
}