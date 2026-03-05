package com.application.polarapplication.ui.theme.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import kotlin.math.roundToInt

val AppBackground = Color(0xFF0D0D12)
val CardSurfaceDark = Color(0xFF15151C)
val NeonBlue = Color(0xFF00E5FF)
val NeonPink = Color(0xFFFF5252)
val NeonGreen = Color(0xFF69F0AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current

    val savedAge by viewModel.profileManager.age.collectAsState()
    val savedWeight by viewModel.profileManager.weight.collectAsState()
    val savedHeight by viewModel.profileManager.height.collectAsState()
    val savedGender by viewModel.profileManager.gender.collectAsState()
    val savedRhr by viewModel.profileManager.rhr.collectAsState()
    val savedCustomHrMax by viewModel.profileManager.customHrMax.collectAsState()
    val savedImageUri by viewModel.profileManager.profileImageUri.collectAsState()


    var age by remember(savedAge) { mutableStateOf(savedAge) }
    var weight by remember(savedWeight) { mutableStateOf(savedWeight) }
    var height by remember(savedHeight) { mutableStateOf(savedHeight) }
    var gender by remember(savedGender) { mutableStateOf(savedGender) }
    var rhr by remember(savedRhr) { mutableStateOf(savedRhr) }
    var customHrMax by remember(savedCustomHrMax) { mutableStateOf(savedCustomHrMax) }
    var profileImageUri by remember(savedImageUri) { mutableStateOf(savedImageUri) }
    var userName by remember { mutableStateOf("Nume Utilizator") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
                profileImageUri = uri.toString()
            }
        }
    )

    val calculatedHrMax = (208 - (0.7 * age)).roundToInt()
    val displayHrMax = customHrMax ?: calculatedHrMax



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // --- HEADER ---
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Cutia Avatarului (acum e clickabilă!)
            Box(
                modifier = Modifier
                    .size(70.dp) // Am făcut-o puțin mai mare
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(NeonBlue.copy(alpha = 0.2f), Color.Transparent)))
                    .border(2.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable {
                        // Când dai click, deschidem galeria doar pentru imagini
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    // Dacă avem o imagine selectată, o afișăm frumos cu Coil
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Poză de Profil",
                        contentScale = ContentScale.Crop, // Face crop ca să umple cutia perfect
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Face, contentDescription = "Avatar", tint = NeonBlue, modifier = Modifier.size(32.dp))
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Adaugă",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(NeonPink, RoundedCornerShape(50))
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column {
                BasicTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                Text("Apasă pe poză pentru a edita", color = Color.Gray, fontSize = 12.sp) // Am modificat textul ca hint
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- DATE BIOMETRICE (Grid 2x2) ---
        Text("METRICI CORP", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricEditorCard(
                title = "Vârstă", value = "$age", unit = "ani", color = NeonBlue, modifier = Modifier.weight(1f),
                onDecrease = { if (age > 14) age-- }, onIncrease = { if (age < 100) age++ }
            )
            MetricEditorCard(
                title = "Greutate", value = "${weight.toInt()}", unit = "kg", color = NeonBlue, modifier = Modifier.weight(1f),
                onDecrease = { if (weight > 40) weight -= 1f }, onIncrease = { if (weight < 150) weight += 1f }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricEditorCard(
                title = "Înălțime", value = "$height", unit = "cm", color = NeonBlue, modifier = Modifier.weight(1f),
                onDecrease = { if (height > 140) height-- }, onIncrease = { if (height < 220) height++ }
            )

            // Selector simplu de sex
            Box(
                modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp)).background(CardSurfaceDark).clickable { gender = if (gender == "Masculin") "Feminin" else "Masculin" },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sex", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(gender, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- PARAMETRI CARDIACI ---
        Text("CAPACITATE CARDIACĂ", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // Card RHR
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Resting Heart Rate (RHR)", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Pulsul la repaus absolut", color = Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (rhr > 30) rhr-- }) { Text("-", color = NeonGreen, fontSize = 24.sp) }
                    Text("$rhr bpm", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = { if (rhr < 100) rhr++ }) { Text("+", color = NeonGreen, fontSize = 24.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card HRmax
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Max Heart Rate (HRmax)", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (customHrMax == null) "Calculat (Formula Tanaka)" else "Setat manual", color = NeonPink.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { customHrMax = displayHrMax - 1 }) { Text("-", color = NeonPink, fontSize = 24.sp) }
                        Text("$displayHrMax bpm", color = NeonPink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        IconButton(onClick = { customHrMax = displayHrMax + 1 }) { Text("+", color = NeonPink, fontSize = 24.sp) }
                    }
                }
                if (customHrMax != null) {
                    TextButton(onClick = { customHrMax = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Resetează la Auto-calcul (${calculatedHrMax})", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveUserProfile(age, weight, height, gender, rhr, customHrMax, profileImageUri)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("SALVEAZĂ PROFILUL", color = AppBackground, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MetricEditorCard(
    title: String, value: String, unit: String, color: Color,
    modifier: Modifier = Modifier, onDecrease: () -> Unit, onIncrease: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(" $unit", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onDecrease() }, contentAlignment = Alignment.Center) {
                    Text("-", color = color, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onIncrease() }, contentAlignment = Alignment.Center) {
                    Text("+", color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}