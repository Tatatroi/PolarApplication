package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HrZoneVerticalBar(
    currentHr: Int,
    maxHr: Int
) {
    // 1. Calculăm procentajul
    val hrPercentage = (currentHr.toFloat() / maxHr.toFloat()).coerceIn(0f, 1f)

    // 2. Zonele de efort conform culorilor tale din grafic
    val (zoneText, zoneColor) = when {
        hrPercentage >= 0.90f -> "Z5 MAXIMUM" to Color(0xFFD32F2F)
        hrPercentage >= 0.80f -> "Z4 ANAEROB" to Color(0xFFF57C00)
        hrPercentage >= 0.70f -> "Z3 AEROBIC" to Color(0xFF388E3C)
        hrPercentage >= 0.60f -> "Z2 CONTROL" to Color(0xFF1976D2)
        else -> "Z1 RECOVERY" to Color(0xFF9E9E9E)
    }

    val animatedHeight by animateFloatAsState(targetValue = hrPercentage, animationSpec = tween(500))
    val animatedColor by animateColorAsState(targetValue = zoneColor, animationSpec = tween(500))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp)
    ) {
        // Textul zonei sus
        Text(
            text = zoneText,
            color = animatedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Containerul Barei (Track-ul)
        Box(
            modifier = Modifier
                .width(28.dp) // Lățime optimizată pentru a încăpea lângă omuleț
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f)), // Fundal foarte discret
            contentAlignment = Alignment.BottomCenter
        ) {
            // Nivelul de umplere cu Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(animatedColor, animatedColor.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Valoarea pulsului jos
        Text(
            text = "$currentHr",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "BPM",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}