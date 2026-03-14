package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ui.stressPredictor.StressBodyVisualizer
import com.application.polarapplication.ui.stressPredictor.StressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: DashboardViewModel,
    userGender: String,
    userMaxHr: Int,
    onMinimizeClick: () -> Unit
) {
    val vitals by viewModel.athleteVitals.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()

    // State pentru confirmarea opririi
    var showStopConfirmation by remember { mutableStateOf(false) }

    // Mapăm textul din vitals către Int-ul cerut de StressIndicator (0 = Calm, 1 = Stres)
    val stressLevelInt = if (vitals.stressLevel.toString().contains("Stres", ignoreCase = true)) 1 else 0

    // Închide ecranul automat dacă antrenamentul a fost oprit din altă parte
    if (!isWorkoutActive) {
        LaunchedEffect(Unit) { onMinimizeClick() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sesiune Live",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMinimizeClick) {
                        Icon(Icons.Default.Close, contentDescription = "Minimizează")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF15151C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0D0D12) // Fundalul Deep Dark consistent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. ZONA DE BIO-FEEDBACK (Puls + Siluetă AI) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stânga: Bara de Puls Animată
                Box(
                    modifier = Modifier.weight(0.35f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    HrZoneVerticalBar(
                        currentHr = vitals.heartRate,
                        maxHr = userMaxHr
                    )
                }

                // Dreapta: Omulețul AI care își schimbă culoarea
                Box(
                    modifier = Modifier.weight(0.65f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    StressBodyVisualizer(
                        stressScore = vitals.stressScore,
                        userGender = userGender
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. INDICATOR STARE (AI Analysis Card) ---
            StressIndicator(stressLevel = stressLevelInt)

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. DASHBOARD METRICI (TRIMP, Calorii, CNS) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSquareCard(
                    label = "TRIMP",
                    value = "%.1f".format(vitals.trimpScore),
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MetricSquareCard(
                    label = "CALORII",
                    value = "${vitals.calories}",
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
                MetricSquareCard(
                    label = "CNS LIVE",
                    value = "${vitals.cnsScore}",
                    unit = "%",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 4. BUTON STOP CU DESIGN PREMIUM ---
            Button(
                onClick = { showStopConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "ÎNCHEIE ANTRENAMENTUL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- DIALOG DE CONFIRMARE ---
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            containerColor = Color(0xFF1E1E24),
            title = { Text("Finalizare sesiune", color = Color.White) },
            text = { Text("Ești sigur că vrei să oprești monitorizarea acum?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopWorkout("BOMPA_SESSION")
                    showStopConfirmation = false
                    onMinimizeClick()
                }) {
                    Text("DA, OPREȘTE", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("CONTINUĂ", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun MetricSquareCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (unit.isNotEmpty()) {
                Text(text = unit, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}