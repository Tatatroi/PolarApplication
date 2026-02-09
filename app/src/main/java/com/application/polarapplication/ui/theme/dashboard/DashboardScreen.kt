package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ai.model.AthleteVitals
import com.application.polarapplication.ai.model.DeviceState
import com.application.polarapplication.model.Workout
import com.application.polarapplication.ui.theme.Indigo

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            GreetingHeader(userName = "Mitroi Stefan")
            Text(
                text = "srefanmitroi@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // 2. Polar Status Card (Acum folosim obiectele curate)
        PolarStatusCard(
            device = uiState.device,
            vitals = uiState.vitals,
            onConnectClick = {
                viewModel.toggleConnection("A6FC0B2E")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.device.isConnected) {
            WorkoutControlPanel(
                isActive = uiState.isWorkoutActive,
                vitals = uiState.vitals,
                onStart = { viewModel.startWorkout() },
                onStop = { type -> viewModel.stopWorkout(type) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Phase Cards
        Row(modifier = Modifier.fillMaxWidth()) {
            PhaseCard(
                title = "Faza Curentă",
                value = "Pregătire",
                icon = Icons.Default.Info,
                color = Indigo,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            PhaseCard(
                title = "Obiectiv",
                value = "Forță",
                icon = Icons.Default.Star,
                color = Color(0xFFF1F1F1),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Workout Details
        WorkoutDetailsSection()
    }
}

@Composable
fun PolarStatusCard(
    device: DeviceState,
    vitals: AthleteVitals,
    onConnectClick: () -> Unit
) {
    // Culorile se schimbă în funcție de starea tehnică a senzorului
    val containerColor = if (device.isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val statusTextColor = if (device.isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = if (device.isConnected) Color.Red else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                // Starea Conexiunii
                Text(
                    text = if (device.isConnected) "Senzor Conectat" else "Senzor Deconectat",
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor
                )

                if (device.isConnected) {
                    // Datele Biologice (Vitals)
                    Text(
                        text = "${vitals.heartRate} BPM (Zona ${vitals.trainingZone})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "HRV (RMSSD): ${"%.1f".format(vitals.rmssd)} ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Status: ${vitals.getBompaReadiness()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Indigo,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Apasă pentru deconectare",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "Apasă pentru a scana...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ... Restul funcțiilor (GreetingHeader, PhaseCard, WorkoutDetailsSection) rămân neschimbate ...
// Asigură-te că le copiezi dacă nu sunt deja în fișier.
@Composable
fun GreetingHeader(userName: String) {
    val firstName = userName.split(" ")[0]
    Text(
        text = "Salut, $firstName!",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun PhaseCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val isDark = color == Indigo
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon, contentDescription = title,
                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Indigo,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Gray)
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color.White else Color.DarkGray)
            }
        }
    }
}

@Composable
fun WorkoutDetailsSection() {
    val todayWorkout = Workout(
        title = "Antrenament Explozivitate",
        duration = "45 min",
        focus = "Viteză și Forță"
    )
    WorkoutDetailsCard(workout = todayWorkout)
}

@Composable
fun WorkoutDetailsCard(workout: Workout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Antrenamentul de Azi:", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(workout.title, color = Indigo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Durată: ", fontWeight = FontWeight.Medium)
                Text(workout.duration)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Focus: ", fontWeight = FontWeight.Medium)
                Text(workout.focus, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray)
        }
    }
}

@Composable
fun WorkoutControlPanel(
    isActive: Boolean,
    vitals: AthleteVitals,
    onStart: () -> Unit,
    onStop: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                // ECRAN PRE-START
                Text(
                    text = "Bazat pe CNS (${vitals.cnsScore}), recomandăm: Forță Maximă",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    Text("START ANTRENAMENT")
                }
            } else {
                // ECRAN ÎN TIMPUL ANTRENAMENTULUI
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TRIMP", style = MaterialTheme.typography.bodySmall)
                        Text("${"%.1f".format(vitals.trimpScore)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CALORII", style = MaterialTheme.typography.bodySmall)
                        Text("${vitals.calories}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CNS LIVE", style = MaterialTheme.typography.bodySmall)
                        Text("${vitals.cnsScore}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Button(
                    onClick = { onStop("Forță") }, // Momentan trimitem hardcoded, putem pune un meniu
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("STOP ȘI SALVEAZĂ")
                }
            }
        }
    }
}