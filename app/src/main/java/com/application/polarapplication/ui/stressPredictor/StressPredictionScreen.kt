package com.application.polarapplication.ui.stressPredictor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.application.polarapplication.ai.analysis.StressManager
import com.application.polarapplication.ui.planning.AppBackground
import com.application.polarapplication.ui.planning.CardSurfaceDark
import com.application.polarapplication.ui.planning.NeonBlue

@Composable
fun StressPredictionScreen(stressManager: StressManager) {
    var selectedTimeFrame by remember { mutableStateOf("Prezent") }
    val options = listOf("Prezent", "Mâine", "7 Zile", "Concurs")

    // Aici simulăm scorul de stres.
    // În realitate, 'Prezent' vine din senzor, restul din algoritmul tău AI.
    val displayScore = when (selectedTimeFrame) {
        "Prezent" -> 0.2f // Exemplu: Calm acum
        "Mâine" -> 0.45f // Exemplu: Stres mediu estimat
        "7 Zile" -> 0.8f // Exemplu: Acumulare oboseală mare
        "Concurs" -> 0.1f // Exemplu: Tapering reușit, ești fresh
        else -> 0.0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PREDICȚIE BIOMETRICĂ",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // 1. VIZUALIZAREA (Silueta cu Aura)
        StressBodyVisualizer(stressScore = displayScore, userGender = "Feminin")

        Spacer(modifier = Modifier.height(32.dp))

        // 2. SELECTORUL DE TIMP (Meniul de jos)
        Text(
            text = "ORIZONT TEMPORAL",
            color = NeonBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurfaceDark, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedTimeFrame == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonBlue.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) NeonBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTimeFrame = option },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) NeonBlue else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
