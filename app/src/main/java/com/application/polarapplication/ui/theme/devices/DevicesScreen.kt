package com.application.polarapplication.ui.theme.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel

@Composable
fun DevicesScreen(viewModel: DashboardViewModel = viewModel()) {
    val devices by viewModel.availableDevices.collectAsState()
    val deviceState by viewModel.uiState.collectAsState()

    // Pornim scanarea când intrăm pe ecran
    DisposableEffect(Unit) {
        viewModel.startScanning()
        onDispose { viewModel.stopScanning() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Management Senzori", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        // SECȚIUNEA 1: Senzor Curent / Istoric
        Text("Senzor Istoric", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text("Polar H10 / OH1") },
                supportingContent = { Text(if (deviceState.device.isConnected) "Conectat" else "Deconectat") },
                trailingContent = {
                    if (!deviceState.device.isConnected) Button(onClick = { /* reconectare la ultimul id */ }) { Text("Connect") }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECȚIUNEA 2: Senzori Noi (Scanare)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Senzori Disponibili", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }

        LazyColumn {
            items(devices.toList()) { info ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { viewModel.connectToSelectedDevice(info.deviceId) }
                ) {
                    ListItem(
                        headlineContent = { Text(info.name) },
                        supportingContent = { Text("ID: ${info.deviceId}") },
                        trailingContent = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                }
            }
        }
    }
}
