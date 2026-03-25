package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ui.theme.Indigo

@OptIn(ExperimentalMaterial3Api::class)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFFFF3E0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isActive) "SESIUNE ACTIVĂ" else "PREGĂTIRE ANTRENAMENT",
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) Color(0xFFE65100) else Color.Gray
            )

            if (!isActive) {
                val recommendation = when {
                    vitals.cnsScore >= 80 -> "Sistemul nervos e odihnit. Recomandăm: STRENGTH / SPEED."
                    vitals.cnsScore >= 50 -> "Stare optimă. Recomandăm: ENDURANCE."
                    vitals.cnsScore > 0   -> "Sistem nervos obosit. Recomandăm: RECOVERY."
                    else                  -> "Se analizează starea CNS..."
                }

                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Indigo
                )

                Text(
                    text = "Alege tipul sesiunii:",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workoutTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                onTypeSelected(type)
                            },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    Text("START ANTRENAMENT")
                }

            } else {
                Text(
                    text = "Tip: $selectedType",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TRIMP", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${"%.1f".format(vitals.trimpScore)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CALORII", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${vitals.calories}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CNS LIVE", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${vitals.cnsScore}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Button(
                    onClick = onMaximizeWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    Text("Detalii Antrenament Live")
                }

                Button(
                    onClick = { onStop(selectedType) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("STOP ȘI SALVEAZĂ")
                }
            }
        }
    }
}