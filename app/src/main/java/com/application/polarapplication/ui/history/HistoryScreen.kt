package com.application.polarapplication.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.theme.Indigo
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun HistoryScreen(viewModel: DashboardViewModel = viewModel(),onSessionClick: (TrainingSessionEntity) -> Unit) {
    val sessions by viewModel.allSessions.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Istoric Antrenamente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Indigo,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nu ai antrenamente salvate.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions) { session ->
                    HistoryCard(
                        session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(session: TrainingSessionEntity, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{onClick()},
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = session.type, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Indigo)
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F1F1))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HistoryMetric(label = "TRIMP", value = "%.1f".format(session.finalTrimp), color = Color(0xFFE65100))
                HistoryMetric(label = "Avg BPM", value = "${session.avgHeartRate}", color = Color(0xFFC62828))
                HistoryMetric(label = "CNS Final", value = "${session.cnsScoreAtEnd}%", color = Color(0xFF2E7D32))
            }

            // Buton temporar pentru a indica faptul că avem grafic salvat
            if (session.hrSamples.isNotEmpty()) {
                Text(
                    text = "📊 Grafic puls disponibil",
                    style = MaterialTheme.typography.labelSmall,
                    color = Indigo.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
    }
}