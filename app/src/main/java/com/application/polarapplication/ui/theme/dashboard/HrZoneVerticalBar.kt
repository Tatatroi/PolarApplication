package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HrZoneVerticalBar(
    currentHr: Int,
    maxHr: Int = 200 // Aici vei pune maxHr-ul setat pentru utilizator
) {
    // 1. Calculăm procentajul pulsului (0.0 la 1.0)
    val hrPercentage = (currentHr.toFloat() / maxHr.toFloat()).coerceIn(0f, 1f)

    // 2. Determinăm zona și culoarea bazată pe procentaj
    val (zoneText, zoneColor) = when {
        hrPercentage >= 0.90f -> "Z5 - VO2max" to Color(0xFFD32F2F) // Roșu
        hrPercentage >= 0.80f -> "Z4 - Anaerob" to Color(0xFFF57C00) // Portocaliu
        hrPercentage >= 0.70f -> "Z3 - Aerob Mod." to Color(0xFF388E3C) // Verde
        hrPercentage >= 0.60f -> "Z2 - Aerob Ușor" to Color(0xFF1976D2) // Albastru
        hrPercentage >= 0.50f -> "Z1 - Recuperare" to Color(0xFF9E9E9E) // Gri
        else -> "Sub Z1" to Color(0xFFE0E0E0) // Gri foarte deschis (repaus)
    }

    // 3. Animăm nivelul de umplere și culoarea pentru o tranziție fluidă
    val animatedHeight by animateFloatAsState(
        targetValue = hrPercentage,
        animationSpec = tween(durationMillis = 500),
        label = "BarHeightAnimation"
    )
    val animatedColor by animateColorAsState(
        targetValue = zoneColor,
        animationSpec = tween(durationMillis = 500),
        label = "BarColorAnimation"
    )

    // 4. Randarea UI-ului
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        // Textul cu Zona în stânga
        Text(
            text = zoneText,
            color = animatedColor,
            fontSize = 16.sp,
            modifier = Modifier.width(120.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Bara propriu-zisă
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(300.dp) // Înălțimea totală a barei
                .clip(RoundedCornerShape(20.dp))
                .background(Color.DarkGray.copy(alpha = 0.2f)), // Background track
            contentAlignment = Alignment.BottomCenter
        ) {
            // Partea umplută a barei
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeight) // Aici se aplică umplerea (0.0 - 1.0)
                    .clip(RoundedCornerShape(20.dp))
                    .background(animatedColor)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Afișăm BPM-ul curent în dreapta
        Text(
            text = "$currentHr\nBPM",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}