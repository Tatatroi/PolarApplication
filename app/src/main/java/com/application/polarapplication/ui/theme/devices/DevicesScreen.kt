package com.application.polarapplication.ui.theme.devices

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.R
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import com.application.polarapplication.R

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0xFF111118)
private val GlassBorder = Color(0x17FFFFFF)
private val GlassSmBg = Color(0xFF141420)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentRed = Color(0xFFF87171)
private val AccentAmber = Color(0xFFFBBF24)

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────
private fun hasBluetoothPermissions(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun isBluetoothEnabled(): Boolean {
    return BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
}

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────
@Composable
fun DevicesScreen(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val devices by viewModel.availableDevices.collectAsState()
    val deviceState by viewModel.uiState.collectAsState()
    val lastDeviceId by viewModel.lastConnectedDeviceId.collectAsState()
    val lastDeviceName by viewModel.lastConnectedDeviceName.collectAsState()

    var hasPermissions by remember { mutableStateOf(hasBluetoothPermissions(context)) }
    var bluetoothEnabled by remember { mutableStateOf(isBluetoothEnabled()) }
    var isScanning by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions && bluetoothEnabled) {
            isScanning = true
            viewModel.startScanning()
        }
    }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            bluetoothEnabled = true
                            if (hasPermissions && !deviceState.device.isConnected) {
                                isScanning = true
                                viewModel.startScanning()
                            }
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            bluetoothEnabled = false
                            isScanning = false
                            viewModel.stopScanning()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
            viewModel.stopScanning()
            isScanning = false
        }
    }

    val isConnected = deviceState.device.isConnected

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.stopScanning()
            isScanning = false
        }
    }

    // Auto-request permissions when entering screen
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        } else if (bluetoothEnabled && !deviceState.device.isConnected) {
            isScanning = true
            viewModel.startScanning()
        }
    }

    // Stop scanning when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
            isScanning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Header
        Text(
            text = "Sensor Setup",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Connect your Polar heart rate sensor",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp
        )

    // Stop scanning when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
            isScanning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── Stare permisiuni / bluetooth ────────────────────────────────────
        if (!hasPermissions) {
            WarningCard(
                icon = Icons.Default.BluetoothDisabled,
                title = "Bluetooth permissions required",
                message = "The app needs Bluetooth permissions to find and connect to your Polar sensor.",
                actionLabel = "Grant Permissions",
                actionColor = AccentIndigo,
                onAction = { permissionLauncher.launch(permissionsToRequest) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!bluetoothEnabled) {
            WarningCard(
                icon = Icons.Default.BluetoothDisabled,
                title = "Bluetooth is off",
                message = "Turn on Bluetooth to scan for nearby Polar sensors.",
                actionLabel = "Open Settings",
                actionColor = AccentAmber,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Senzor conectat / ultimul conectat ──────────────────────────────
        if (deviceState.device.isConnected) {
            SectionLabel("CONNECTED SENSOR")
            Spacer(modifier = Modifier.height(8.dp))
            ConnectedSensorCard(
                deviceId = deviceState.device.deviceId,
                batteryLevel = deviceState.device.batteryLevel,
                onDisconnect = { viewModel.toggleConnection(deviceState.device.deviceId) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        } else if (!lastDeviceId.isNullOrEmpty()) {
            SectionLabel("LAST CONNECTED")
            Spacer(modifier = Modifier.height(8.dp))
            LastSensorCard(
                deviceName = lastDeviceName ?: "Polar Sensor",
                deviceId = lastDeviceId!!,
                onReconnect = {
                    if (hasPermissions && bluetoothEnabled) {
                        viewModel.connectToSelectedDevice(lastDeviceId!!)
                    } else if (!hasPermissions) {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Scanare dispozitive ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel(
                if (isScanning && hasPermissions && bluetoothEnabled) {
                    "SCANNING NEARBY..."
                } else {
                    "AVAILABLE SENSORS"
                }
            )
            if (isScanning && hasPermissions && bluetoothEnabled) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = AccentIndigo
                )
            } else if (hasPermissions && bluetoothEnabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentIndigo.copy(alpha = 0.1f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable {
                            isScanning = true
                            viewModel.startScanning()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Scan", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasPermissions || !bluetoothEnabled) {
            // Placeholder când nu avem permisiuni
            EmptyStateCard(
                icon = Icons.Default.Bluetooth,
                message = "Fix the issues above to scan for sensors"
            )
        } else if (devices.isEmpty() && isScanning) {
            EmptyStateCard(
                icon = Icons.Default.Search,
                message = "Looking for nearby Polar sensors...\nMake sure your sensor is powered on and close."
            )
        } else if (devices.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.BluetoothSearching,
                message = "No sensors found. Tap 'Scan' to search again."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices.toList()) { info ->
                    val isAlreadyConnected = deviceState.device.isConnected &&
                        deviceState.device.deviceId == info.deviceId

                    AvailableSensorCard(
                        name = info.name,
                        deviceId = info.deviceId,
                        isConnected = isAlreadyConnected,
                        onClick = {
                            if (!isAlreadyConnected) {
                                viewModel.connectToSelectedDevice(info.deviceId)
                                // Salvează ultimul dispozitiv conectat
                                viewModel.saveLastConnectedDevice(info.deviceId, info.name)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE
// ─────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.25f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun WarningCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    actionColor: Color,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(actionColor.copy(alpha = 0.07f))
            .border(1.dp, actionColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = actionColor, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(actionColor.copy(alpha = 0.15f))
                .border(1.dp, actionColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { onAction() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(actionLabel, color = actionColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConnectedSensorCard(
    deviceId: String,
    batteryLevel: Int,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentGreen.copy(alpha = 0.07f))
            .border(1.dp, AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(72.dp) // mai lat pentru că imaginea e landscape
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.polarsenzor),
                contentDescription = "Polar Sensor",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Text("Connected", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("ID: $deviceId", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (batteryLevel > 0) {
                Text(
                    "Battery: $batteryLevel%",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        } else {
            Icon(
                Icons.Default.Add,
                null,
                tint     = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AccentRed.copy(alpha = 0.1f))
                .border(1.dp, AccentRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .clickable { onDisconnect() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Disconnect", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LastSensorCard(
    deviceName: String,
    deviceId: String,
    onReconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.polarsenzor),
                contentDescription = "Polar Sensor",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .alpha(0.4f) // mai estompat când e deconectat
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(deviceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("ID: $deviceId", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
            Text("Not connected", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AccentIndigo.copy(alpha = 0.1f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .clickable { onReconnect() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Reconnect", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AvailableSensorCard(
    name: String,
    deviceId: String,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = if (isConnected) AccentGreen else AccentIndigo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassBg)
            .border(
                1.dp,
                if (isConnected) AccentGreen.copy(alpha = 0.3f) else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isConnected) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothSearching,
                null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name.ifBlank { "Unknown Sensor" },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "ID: $deviceId",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
        if (isConnected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGreen.copy(alpha = 0.12f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Connected", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        } else {
            Icon(
                Icons.Default.Add,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
