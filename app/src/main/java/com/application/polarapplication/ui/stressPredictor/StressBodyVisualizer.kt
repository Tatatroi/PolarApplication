package com.application.polarapplication.ui.stressPredictor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.application.polarapplication.R
@Composable
fun StressBodyVisualizer(
    stressScore: Float,
    userGender: String
) {
    // 1. Animăm mărimea aurei (0.0 -> 1.0)
    val animatedRadiusMultiplier by animateFloatAsState(
        targetValue = stressScore,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AuraSize"
    )

    // 2. Alegem culoarea aurei
    val auraColor = when {
        stressScore < 0.35f -> Color(0xFF00FF94) // Verde
        stressScore < 0.70f -> Color(0xFFFFD600) // Galben
        else -> Color(0xFFFF3D00) // Roșu
    }

    // 3. Resursa grafică
    val bodyPainter = if (userGender == "Feminin") {
        painterResource(id = R.drawable.womanclear)
    } else {
        painterResource(id = R.drawable.clearman)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Ne asigurăm că are o înălțime clară ca să nu se "turtsească"
            .height(400.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // STRATUL 1: Aura Strălucitoare în spate
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPoint = center
            val maxRadius = size.minDimension / 1.5f
            // Când scorul e 0, vrem totuși o aură mică vizibilă
            val currentRadius = maxRadius * (animatedRadiusMultiplier.coerceAtLeast(0.2f))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor.copy(alpha = 0.6f), Color.Transparent),
                    center = centerPoint,
                    radius = currentRadius
                ),
                radius = currentRadius
            )
        }

        Image(
            painter = bodyPainter,
            contentDescription = "Siluetă Biometrică",
            modifier = Modifier
                .fillMaxHeight(0.9f) // Lăsăm puțin spațiu sus/jos
                .fillMaxWidth(0.9f)
        )
    }
}
