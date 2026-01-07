package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.application.polarapplication.ui.theme.Indigo
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.HorizontalDivider// Note: Divider is now HorizontalDivider in M3
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import com.application.polarapplication.model.Workout

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {

    val isConnected by viewModel.isConnected.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header with Name and Email
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            GreetingHeader(userName = "Mitroi Stefan")
            Text(
                text = "srefanmitroi@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // 2. Polar Sensor Status Card
        // Use the variables observed from your ViewModel
        PolarStatusCard(
            isConnected = isConnected, // now uses the variable from viewModel
            heartRate = heartRate,     // now uses the variable from viewModel
            onConnectClick = {
                viewModel.toggleConnection("A6FC0B2E")
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 3. Phase Cards (Bompa Theory: Macro/Meso cycles)
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

        // 4. Workout Details (Uncommented and improved)
        WorkoutDetailsSection()
    }
}


@Composable
fun PolarStatusCard(isConnected: Boolean, heartRate: Int, onConnectClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable{ onConnectClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = if (isConnected) Color.Red else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isConnected) "Senzor Conectat" else "Senzor Deconectat",
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                if (isConnected) {
                    Text(
                        text = "$heartRate BPM",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(text = "Apasă pentru a scana...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
/**
 * GreetingHeader(name)
 * Antetul principal de întâmpinare.
 */
@Composable
fun GreetingHeader(userName: String) {
    val firstName = userName.split(" ")[0]
    Text(
        text = "Salut, $firstName!",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}


/**
 * 🔹 PhaseCard(title, value, icon, color)
 * Cardurile mici pentru faza curentă și obiectivul zilnic.
 */
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
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Indigo,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.DarkGray
                )
            }
        }
    }
}


/**
 * 🔹 WorkoutDetailsCard(workout)
 * Cardul mare care afișează detaliile antrenamentului planificat.
 */
@Composable
fun WorkoutDetailsCard(workout: Workout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Antrenamentul de Azi:",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = workout.title,
                color = Indigo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Detalii Durată & Focus
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.List,contentDescription = "Durată",tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Durată Estimată:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = workout.duration)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = "Focus", tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Focus Monitorizat (Senzor):", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = workout.focus, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray)
        }
    }
}

@Composable
fun WorkoutDetailsSection() {
    // For now, we use "dummy" data to test the UI
    val todayWorkout = Workout(
        title = "Antrenament Explozivitate",
        duration = "45 min",
        focus = "Viteză și Forță (Teoria lui Bompa)"
    )
    WorkoutDetailsCard(workout = todayWorkout)
}

