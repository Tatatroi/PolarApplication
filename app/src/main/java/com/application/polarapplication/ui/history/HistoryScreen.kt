package com.application.polarapplication.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Paleta de Culori Exacte din Design ---
val AppBackground = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)

// Helper pentru a extrage tema în funcție de antrenament
fun getThemeForWorkout(type: String): WorkoutTheme {
    return when (type.uppercase()) {
        "STRENGTH" -> WorkoutTheme(Color(0xFFFF3B30), "High Load")
        "ENDURANCE" -> WorkoutTheme(Color(0xFF34C759), "Aerobic Base")
        "SPEED" -> WorkoutTheme(Color(0xFFFF9500), "Anaerobic Load")
        "RECOVERY" -> WorkoutTheme(Color(0xFF007AFF), "Active Recovery")
        else -> WorkoutTheme(Color(0xFF00E5FF), "General Load")
    }
}

data class WorkoutTheme(val color: Color, val label: String)

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel(),
    onSessionClick: (TrainingSessionEntity) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Istoric Antrenamente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
        )

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nicio sesiune înregistrată.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(sessions, key = { it.id }) { session ->
                    PremiumHistoryCard(
                        session = session,
                        onClick = { onSessionClick(session) },
                        onDelete = { viewModel.deleteSession(session) }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) } // Padding jos
            }
        }
    }
}

@Composable
fun PremiumHistoryCard(
    session: TrainingSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(session.date))
    val theme = getThemeForWorkout(session.type)

    val cardBackgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            theme.color.copy(alpha = 0.15f),
            CardSurfaceDark,
            theme.color.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(theme.color.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Iconița de ștergere (Rămâne sus în dreapta)
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .offset(x = 8.dp, y = (-8).dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Șterge", tint = Color.Gray.copy(alpha = 0.5f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- PARTEA STÂNGĂ (Detalii) ---
            Column(modifier = Modifier.weight(1f)) {

                // === MODIFICAREA ESTE AICI ===
                // Header (Titlu și Dată puse unul sub altul, nu unul lângă altul)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = session.type.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        // Adăugăm padding în dreapta ca titlul lung să nu intre peste iconița de delete
                        modifier = Modifier.padding(end = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                // ==============================

                Spacer(modifier = Modifier.height(24.dp))

                // Metrici Principale
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Avg HR
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avg HR", color = Color.Gray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${session.avgHeartRate}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(" bpm", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                        }
                    }
                    // TRIMP
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TRIMP Score", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "%.1f".format(session.finalTrimp),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bara segmentată de intensitate
                IntensitySegmentedBar(color = theme.color, trimp = session.finalTrimp.toFloat())

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(theme.color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = theme.label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // --- PARTEA DREAPTĂ (CNS Ring) ---
            CnsCircularRing(cnsValue = session.cnsScoreAtEnd, color = theme.color)
        }
    }
}

// Custom Composable pentru inelul luminos CNS
@Composable
fun CnsCircularRing(cnsValue: Int, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        // MODIFICAREA E AICI: Mai întâi padding, apoi mărime fixă + aspectRatio
        modifier = Modifier
            .padding(start = 8.dp)
            .size(85.dp)
            .aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()

            // Inelul de fundal
            drawArc(
                color = Color.DarkGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Inelul colorat luminos
            val progressAngle = (cnsValue / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.4f), color)),
                startAngle = -90f,
                sweepAngle = progressAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Textul central
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$cnsValue%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "CNS Final",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
// Custom Composable pentru bara segmentată
@Composable
fun IntensitySegmentedBar(color: Color, trimp: Float) {
    val percentage = (trimp / 150f).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)) {
        val segmentWidth = 3.dp.toPx()
        val segmentSpacing = 3.dp.toPx()
        val totalSegments = (size.width / (segmentWidth + segmentSpacing)).toInt()
        val activeSegments = (totalSegments * percentage).toInt()

        for (i in 0 until totalSegments) {
            val isActive = i < activeSegments
            val alpha = if (isActive) 1f - (i.toFloat() / totalSegments * 0.5f) else 0.1f

            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(i * (segmentWidth + segmentSpacing), 0f),
                size = Size(segmentWidth, size.height)
            )
        }
    }
}