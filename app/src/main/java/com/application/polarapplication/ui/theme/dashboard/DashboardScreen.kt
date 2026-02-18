package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            onDisconnectClick = {
                // Dacă e conectat, trimitem ID-ul dispozitivului către toggleConnection pentru a-l opri
                if (uiState.device.deviceId.isNotEmpty()) {
                    viewModel.toggleConnection(uiState.device.deviceId)
                }
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
fun PolarStatusCard(
    device: DeviceState,
    vitals: AthleteVitals,
    onDisconnectClick: () -> Unit // Schimbăm numele pentru claritate
) {
    val containerColor = if (device.isConnected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
    val statusTextColor = if (device.isConnected) Color(0xFF2E7D32) else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Click-ul funcționează doar pentru deconectare acum
            .clickable(enabled = device.isConnected) { onDisconnectClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (device.isConnected) {
                // --- STARE CONECTAT ---
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Senzor Activ", fontWeight = FontWeight.Bold, color = statusTextColor)
                    Text(
                        text = "${vitals.heartRate} BPM",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Apasă pentru deconectare",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            } else {
                // --- STARE DECONECTAT (AȘTEPTARE) ---
                // Aici simulăm "imaginea" de așteptare cu un icon și un loader
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 2.dp,
                        color = Indigo.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Așteptare conexiune...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Configurează senzorul din tab-ul 'Senzori'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutControlPanel(
    isActive: Boolean,
    vitals: AthleteVitals,
    onStart: () -> Unit,
    onStop: (String) -> Unit
) {
    // Definirea listei de tipuri
    val workoutTypes = listOf("STRENGTH", "ENDURANCE", "SPEED", "RECOVERY", "REST")

    // Mutăm starea aici sus ca să fie persistentă pe durata cât afișăm cardul
    var selectedType by remember { mutableStateOf(workoutTypes[0]) }

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
                // --- ECRAN PRE-START ---
                val recommendation = when {
                    vitals.cnsScore >= 80 -> "Sistemul nervos e odihnit. Recomandăm: STRENGTH / SPEED."
                    vitals.cnsScore >= 50 -> "Stare optimă. Recomandăm: ENDURANCE."
                    vitals.cnsScore > 0 -> "Sistem nervos obosit. Recomandăm: RECOVERY."
                    else -> "Se analizează starea CNS..."
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

                // Selectorul de tipuri (Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()), // Permite scroll dacă sunt multe
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workoutTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
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
                // --- ECRAN ÎN TIMPUL ANTRENAMENTULUI ---
                // Afișăm tipul curent selectat pentru confirmare
                Text(
                    text = "Tip: $selectedType",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

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
                    // ACUM TRIMITEM TIPUL SELECTAT, NU "FORȚĂ" HARDCODED
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