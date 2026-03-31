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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)
private val GlassSmBg = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentRed = Color(0xFFF87171)
private val AccentIndigo = Color(0xFF818CF8)

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current

    // State din ProfileManager
    val savedDobMillis by viewModel.profileManager.dobMillis.collectAsState()
    val savedWeight by viewModel.profileManager.weight.collectAsState()
    val savedHeight by viewModel.profileManager.height.collectAsState()
    val savedGender by viewModel.profileManager.gender.collectAsState()
    val savedRhr by viewModel.profileManager.rhr.collectAsState()
    val savedCustomHrMax by viewModel.profileManager.customHrMax.collectAsState()
    val savedImageUri by viewModel.profileManager.profileImageUri.collectAsState()
    val savedAvailableDays by viewModel.profileManager.availableDays.collectAsState()

    // State local editabil
    var dobMillis by remember(savedDobMillis) { mutableStateOf(savedDobMillis) }
    var weight by remember(savedWeight) { mutableStateOf(savedWeight) }
    var height by remember(savedHeight) { mutableStateOf(savedHeight) }
    var gender by remember(savedGender) { mutableStateOf(savedGender) }
    var rhr by remember(savedRhr) { mutableStateOf(savedRhr) }
    var customHrMax by remember(savedCustomHrMax) { mutableStateOf(savedCustomHrMax) }
    var profileImageUri by remember(savedImageUri) { mutableStateOf(savedImageUri) }
    var availableDays by remember(savedAvailableDays) { mutableStateOf(savedAvailableDays) }
    var userName by remember { mutableStateOf("Nume Utilizator") }

    // DatePicker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dobMillis ?: System.currentTimeMillis()
    )

    // Calcule derivate din data nașterii
    val age = remember(dobMillis) {
        dobMillis?.let {
            val dob = LocalDate.ofEpochDay(it / 86400000L)
            Period.between(dob, LocalDate.now()).years
        } ?: 24
    }
    val calculatedHrMax = (208 - 0.7 * age).roundToInt()
    val displayHrMax = customHrMax ?: calculatedHrMax

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                profileImageUri = uri.toString()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Blob decorativ
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 80.dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x0D6366F1), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Header ──────────────────────────────────────────────────────
            ProfileHeader(
                userName = userName,
                profileImageUri = profileImageUri,
                onNameChange = { userName = it },
                onAvatarClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Date biometrice ──────────────────────────────────────────────
            SectionLabel("Date biometrice")
            Spacer(modifier = Modifier.height(8.dp))

// Data nașterii — singur pe rând
            DobCard(
                dobMillis = dobMillis,
                age = age,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

// Greutate + Înălțime pe același rând
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricInputCard(
                    label = "GREUTATE",
                    value = "${weight.toInt()}",
                    unit = "kg",
                    modifier = Modifier.weight(1f),
                    onValueChange = { input ->
                        input.toIntOrNull()?.let { v ->
                            if (v in 40..150) weight = v.toFloat()
                        }
                    }
                )
                MetricInputCard(
                    label = "ÎNĂLȚIME",
                    value = "$height",
                    unit = "cm",
                    modifier = Modifier.weight(1f),
                    onValueChange = { input ->
                        input.toIntOrNull()?.let { v ->
                            if (v in 140..220) height = v
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

// Sex — singur pe rând
            GenderCard(
                gender = gender,
                onChange = { gender = if (gender == "Masculin") "Feminin" else "Masculin" },
                modifier = Modifier.fillMaxWidth()
            )
            // ── Capacitate cardiacă ──────────────────────────────────────────
            SectionLabel("Capacitate cardiacă")
            Spacer(modifier = Modifier.height(8.dp))

            // RHR
            CardioStepCard(
                title = "Resting Heart Rate",
                subtitle = "Pulsul la repaus absolut",
                value = "$rhr bpm",
                color = AccentGreen,
                onDecrease = { if (rhr > 30) rhr-- },
                onIncrease = { if (rhr < 100) rhr++ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // HRmax
            CardioStepCard(
                title = "Max Heart Rate (HRmax)",
                subtitle = if (customHrMax == null) "Formula Tanaka · auto" else "Setat manual",
                value = "$displayHrMax bpm",
                color = AccentRed,
                onDecrease = { customHrMax = displayHrMax - 1 },
                onIncrease = { customHrMax = displayHrMax + 1 },
                extraContent = if (customHrMax != null) {
                    {
                        TextButton(
                            onClick = { customHrMax = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                "Resetează la auto ($calculatedHrMax bpm)",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    null
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Zile disponibile ─────────────────────────────────────────────
            SectionLabel("Disponibilitate săptămânală")
            Spacer(modifier = Modifier.height(4.dp))

            // Info Bompa
            BompaInfoBox(
                text = "Microciclu-ul Bompa se adaptează la câte zile ești disponibil. Minimum 3 zile pentru un plan eficient."
            )

            Spacer(modifier = Modifier.height(8.dp))

            AvailableDaysCard(
                availableDays = availableDays,
                onChange = { day, selected ->
                    val newSet = availableDays.toMutableSet()
                    if (selected) newSet.add(day) else newSet.remove(day)
                    // Minim 3 zile
                    if (newSet.size >= 3) availableDays = newSet
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Buton salvare ────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.saveUserProfile(
                        age = age,
                        weight = weight,
                        height = height,
                        gender = gender,
                        rhr = rhr,
                        customHrMax = customHrMax,
                        profileImageUri = profileImageUri,
                        dobMillis = dobMillis,
                        availableDays = availableDays
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.07f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "SALVEAZĂ PROFILUL",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── DatePicker Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dobMillis = datePickerState.selectedDateMillis ?: dobMillis
                    showDatePicker = false
                }) {
                    Text("CONFIRMĂ", color = AccentIndigo)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("ANULEAZĂ", color = Color.Gray)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF111116)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE
// ─────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    userName: String,
    profileImageUri: String?,
    onNameChange: (String) -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassSmBg)
                .border(1.dp, GlassSmBorder, RoundedCornerShape(18.dp))
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Poză de Profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(AccentIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column {
            BasicTextField(
                value = userName,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                ),
                cursorBrush = SolidColor(AccentIndigo),
                singleLine = true
            )
            Text(
                text = "Apasă pe poză pentru a edita",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MetricInputCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    var localValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            BasicTextField(
                value = localValue,
                onValueChange = { input ->
                    // Permite doar cifre
                    if (input.all { it.isDigit() } && input.length <= 3) {
                        localValue = input
                        onValueChange(input)
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                cursorBrush = SolidColor(AccentIndigo),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = unit,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 5.dp, start = 3.dp)
            )
        }
    }
}

@Composable
private fun StepButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(7.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GenderCard(gender: String, onChange: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SEX",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.width(32.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Masculin" to "Male", "Feminin" to "Female").forEach { (value, label) ->
                val isSelected = gender == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                AccentIndigo.copy(alpha = 0.15f)
                            } else {
                                Color.White.copy(alpha = 0.03f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) {
                                AccentIndigo.copy(alpha = 0.4f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { if (!isSelected) onChange() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) {
                            AccentIndigo
                        } else {
                            Color.White.copy(alpha = 0.25f)
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DobCard(
    dobMillis: Long?,
    age: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val dobText = dobMillis?.let {
        LocalDate.ofEpochDay(it / 86400000L).format(fmt)
    } ?: "Selectează data nașterii"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSmBg)
            .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "DATA NAȘTERII",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dobText,
                color = if (dobMillis != null) Color.White else AccentIndigo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dobMillis != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentIndigo.copy(alpha = 0.12f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$age ani",
                        color = AccentIndigo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = AccentIndigo.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CardioStepCard(
    title: String,
    subtitle: String,
    value: String,
    color: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = color.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepButton("−", Modifier.width(28.dp)) { onDecrease() }
                Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
                StepButton("+", Modifier.width(28.dp)) { onIncrease() }
            }
        }
        extraContent?.invoke(this)
    }
}

@Composable
private fun AvailableDaysCard(
    availableDays: Set<Int>,
    onChange: (Int, Boolean) -> Unit
) {
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val selectedCount = availableDays.size

    val hint = when {
        selectedCount < 3 -> "Minimum 3 zile necesare"
        selectedCount == 3 -> "Plan minim Bompa"
        selectedCount == 4 -> "Plan optim Bompa"
        selectedCount == 5 -> "Plan avansat"
        selectedCount == 6 -> "Plan intensiv"
        else -> "Fără zi de odihnă — nerecomandat"
    }
    val hintColor = when {
        selectedCount < 3 -> AccentRed
        selectedCount <= 5 -> AccentGreen
        else -> AccentRed.copy(alpha = 0.7f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zile disponibile / săptămână",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$selectedCount zile",
                color = hintColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            dayLabels.forEachIndexed { index, label ->
                val dayNum = index + 1
                val selected = dayNum in availableDays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selected) {
                                AccentIndigo.copy(alpha = 0.2f)
                            } else {
                                Color.White.copy(alpha = 0.03f)
                            }
                        )
                        .border(
                            1.dp,
                            if (selected) {
                                AccentIndigo.copy(alpha = 0.5f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { onChange(dayNum, !selected) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) AccentIndigo else Color.White.copy(alpha = 0.2f),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = hint,
            color = hintColor.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BompaInfoBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AccentIndigo.copy(alpha = 0.06f))
            .border(0.5.dp, AccentIndigo.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = AccentIndigo.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Text(
            text = text,
            color = AccentIndigo.copy(alpha = 0.7f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.25f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}
