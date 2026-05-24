package com.application.polarapplication.ui.stressPredictor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.application.polarapplication.R

@Composable
fun StressBodyVisualizer(
    stressScore: Float,
    userGender: String,
    auraColor: Color? = null,
    heartRate: Int = 0  // NOU: HR real pentru pulsație sincronizată
) {
    // ── Culoarea aurei ────────────────────────────────────────────────────────
    val computedAuraColor = auraColor ?: when {
        stressScore < 0.35f -> Color(0xFF00FF94)
        stressScore < 0.70f -> Color(0xFFFFD600)
        else                -> Color(0xFFFF3D00)
    }

    // ── Mărimea aurei animată cu intensitatea ─────────────────────────────────
    val animatedRadius by animateFloatAsState(
        targetValue   = stressScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label         = "AuraSize"
    )

    // ── Pulsație ritmică bazată pe HR ─────────────────────────────────────────
    // Dacă avem HR real, calculăm intervalul; altfel folosim 1000ms default
    val pulseDurationMs = if (heartRate > 40) {
        (60000f / heartRate).toInt().coerceIn(400, 1500)
    } else {
        1000
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulsul principal — expansiune și contracție
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(pulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Al doilea inel — offset de fază (bate în contratmp)
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue  = 1.08f,
        targetValue   = 0.88f,
        animationSpec = infiniteRepeatable(
            animation  = tween(pulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale2"
    )

    // Al treilea inel — mai lent, efect de "undă"
    val pulseScale3 by infiniteTransition.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween((pulseDurationMs * 1.5f).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale3"
    )

    // Opacitatea pulsului — mai intens la efort mare
    val baseAlpha = (0.2f + stressScore * 0.4f).coerceIn(0.15f, 0.6f)

    // ── Resursa grafică ───────────────────────────────────────────────────────
    val bodyPainter = if (userGender == "Female") {
        painterResource(id = R.drawable.womanclear)
    } else {
        painterResource(id = R.drawable.clearman)
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx     = center.x
            val cy     = center.y
            val maxR   = size.minDimension / 1.6f
            val baseR  = maxR * (animatedRadius.coerceAtLeast(0.15f))

            // ── Inel 3 — cel mai exterior, cel mai transparent (undă lentă) ──
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        computedAuraColor.copy(alpha = baseAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseR * pulseScale3 * 1.3f
                ),
                radius = baseR * pulseScale3 * 1.3f
            )

            // ── Inel 2 — intermediar, opacitate medie ─────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        computedAuraColor.copy(alpha = baseAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseR * pulseScale2 * 1.1f
                ),
                radius = baseR * pulseScale2 * 1.1f
            )

            // ── Inel 1 — cel mai interior, cel mai opac (pulsul principal) ───
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        computedAuraColor.copy(alpha = baseAlpha * 0.7f),
                        computedAuraColor.copy(alpha = baseAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseR * pulseScale
                ),
                radius = baseR * pulseScale
            )
        }

        // Silueta deasupra aurelor
        Image(
            painter            = bodyPainter,
            contentDescription = "Biometric Silhouette",
            modifier           = Modifier
                .fillMaxHeight(0.95f)
                .fillMaxWidth(0.95f)
        )
    }
}