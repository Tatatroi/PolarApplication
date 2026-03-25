package com.application.polarapplication.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

val AppBackground = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)
val NeonBlue = Color(0xFF00E5FF)
val NeonPink = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSetupScreen(
    viewModel: DashboardViewModel = viewModel(),
    onPlanGenerated: () -> Unit) {
    var competitionDate by remember { mutableStateOf(System.currentTimeMillis() + (86400000 * 30)) } // Default peste 30 zile
    var selectedGoal by remember { mutableStateOf("Maraton / Endurance") }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = competitionDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Planificare Inteligentă",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Configurează competiția pentru algoritmul AI",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- SELECTIE DATA ---
        Text("DATA COMPETIȚIEI", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(16.dp))
                val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(competitionDate))
                Text(text = formattedDate, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SELECTIE OBIECTIV ---
        Text("TIPUL OBIECTIVULUI", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        val goals = listOf("Maraton / Endurance", "Sprinting / Speed", "Powerlifting / Strength", "General Fitness")
        goals.forEach { goal ->
            GoalCard(
                title = goal,
                isSelected = selectedGoal == goal,
                onClick = { selectedGoal = goal }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- BUTON GENERARE ---
        Button(
            onClick = {
                val selectedLocalDate = java.time.Instant
                    .ofEpochMilli(competitionDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                viewModel.setCompetitionDate(selectedLocalDate)
                onPlanGenerated()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("GENEREAZĂ PERIODIZARE BOMPA", color = AppBackground, fontWeight = FontWeight.Black)
        }
    }

    // Modal Date Picker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    competitionDate = datePickerState.selectedDateMillis ?: competitionDate
                    showDatePicker = false
                }) { Text("CONFIRMĂ", color = NeonBlue) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun GoalCard(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) NeonBlue else Color.Transparent
    val bgColor = if (isSelected) NeonBlue.copy(alpha = 0.1f) else CardSurfaceDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonBlue else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
