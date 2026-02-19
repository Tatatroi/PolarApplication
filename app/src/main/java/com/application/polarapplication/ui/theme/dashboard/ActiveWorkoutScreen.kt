package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: DashboardViewModel,
    onMinimizeClick: () -> Unit // Ne întoarcem la Dashboard
) {
    val vitals by viewModel.athleteVitals.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()

    // Dacă utilizatorul oprește antrenamentul de aici sau a fost oprit din Dashboard
    if (!isWorkoutActive) {
        LaunchedEffect(Unit) { onMinimizeClick() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesiune în Desfășurare") },
                navigationIcon = {
                    IconButton(onClick = onMinimizeClick) {
                        Icon(Icons.Default.Close, contentDescription = "Minimizează")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Bara Verticală de Puls și Zone
            HrZoneVerticalBar(
                currentHr = vitals.heartRate,
                maxHr = 200 // Tot aici e hardcoded, o poți scoate din setări mai târziu
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Carduri cu Date în Timp Real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricSquareCard("TRIMP", String.format("%.1f", vitals.trimpScore), modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricSquareCard("Calorii", "${vitals.calories} kcal", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricSquareCard("CNS Live", "${vitals.cnsScore}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Buton de Stop Antrenament (opțional aici, poți lăsa stop-ul doar pe Dashboard)
            Button(
                onClick = {
                    viewModel.stopWorkout("BOMPA_SESSION") // Poți da pass la tipul real dacă îl ai salvat
                    onMinimizeClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Oprește Antrenamentul", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricSquareCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}