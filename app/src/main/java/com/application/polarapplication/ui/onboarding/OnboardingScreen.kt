package com.application.polarapplication.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BgDark        = Color(0xFF080808)
private val AccentIndigo  = Color(0xFF818CF8)
private val AccentGreen   = Color(0xFF4ADE80)
private val AccentAmber   = Color(0xFFFBBF24)
private val GlassSmBg     = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)

// ─────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (name: String, age: Int, gender: String, height: Int, weight: Float, dobMillis: Long?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope      = rememberCoroutineScope()

    var name         by remember { mutableStateOf("") }
    var age          by remember { mutableStateOf(25) }
    var gender       by remember { mutableStateOf("Male") }
    var height       by remember { mutableStateOf(175) }
    var weight       by remember { mutableStateOf(75f) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        HorizontalPager(
            state             = pagerState,
            modifier          = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> ProfilePage(
                    name         = name,
                    age          = age,
                    gender       = gender,
                    height       = height,
                    weight       = weight,
                    selectedDate = selectedDate,
                    onName       = { name = it },
                    onAge        = { age = it },
                    onGender     = { gender = it },
                    onHeight     = { height = it },
                    onWeight     = { weight = it },
                    onDateSelect = { date ->
                        selectedDate = date
                        age = Period.between(date, LocalDate.now()).years
                    },
                    onDone = {
                        val dobMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())
                            ?.toInstant()?.toEpochMilli()
                        onComplete(name, age, gender, height, weight, dobMillis)
                    }
                )
            }
        }

        // Page indicator — doar pe pagina 2
        if (pagerState.currentPage == 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(2) { i ->
                    val isActive = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isActive) AccentIndigo else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PAGE 1 — WELCOME
// ─────────────────────────────────────────────

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter            = painterResource(id = R.drawable.firstimageintro),
            contentDescription = "AthleteIQ",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )
        // Gradient jos
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to Color.Transparent,
                        0.65f to Color.Transparent,
                        0.85f to BgDark.copy(alpha = 0.7f),
                        1.0f  to BgDark
                    )
                )
            )
        )
        // Buton
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentIndigo.copy(alpha = 0.15f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Get Started →",
                color         = AccentIndigo,
                fontSize      = 17.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// PAGE 2 — PROFILE
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePage(
    name:         String,
    age:          Int,
    gender:       String,
    height:       Int,
    weight:       Float,
    selectedDate: LocalDate?,
    onName:       (String) -> Unit,
    onAge:        (Int) -> Unit,
    onGender:     (String) -> Unit,
    onHeight:     (Int) -> Unit,
    onWeight:     (Float) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onDone:       () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text("Tell us about yourself", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(
            "We use this to personalize your training\nand calculate accurate metrics.",
            color      = Color.White.copy(alpha = 0.4f),
            fontSize   = 13.sp,
            lineHeight = 19.sp,
            modifier   = Modifier.padding(top = 6.dp, bottom = 32.dp)
        )

        // ── Name ──────────────────────────────────────────────────────────────
        ProfileSectionLabel("Your Name")
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value         = name,
            onValueChange = onName,
            modifier      = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSmBg)
                .border(
                    1.dp,
                    if (name.isNotEmpty()) AccentIndigo.copy(alpha = 0.4f) else GlassSmBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle     = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush   = SolidColor(AccentIndigo),
            singleLine    = true,
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text("e.g. Alex Johnson", color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                }
                inner()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Date of Birth ─────────────────────────────────────────────────────
        ProfileSectionLabel("Date of Birth")
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSmBg)
                .border(
                    1.dp,
                    if (selectedDate != null) AccentIndigo.copy(alpha = 0.4f) else GlassSmBorder,
                    RoundedCornerShape(14.dp)
                )
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    selectedDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        ?: "Select date of birth",
                    color    = if (selectedDate != null) Color.White else Color.White.copy(alpha = 0.2f),
                    fontSize = 16.sp
                )
                if (selectedDate != null) {
                    Text(
                        "Age: $age",
                        color      = AccentIndigo,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis() -
                        (25L * 365 * 24 * 60 * 60 * 1000)
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelect(date)
                        }
                        showDatePicker = false
                    }) { Text("OK", color = AccentIndigo) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Gender ────────────────────────────────────────────────────────────
        ProfileSectionLabel("Gender")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GenderChip("Male",   gender == "Male",   AccentIndigo, Modifier.weight(1f)) { onGender("Male") }
            GenderChip("Female", gender == "Female", AccentIndigo, Modifier.weight(1f)) { onGender("Female") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Height ────────────────────────────────────────────────────────────
        ProfileSectionLabel("Height")
        Spacer(modifier = Modifier.height(8.dp))
        StepperCard(
            value   = height,
            unit    = "cm",
            color   = AccentGreen,
            onMinus = { if (height > 140) onHeight(height - 1) },
            onPlus  = { if (height < 220) onHeight(height + 1) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Weight ────────────────────────────────────────────────────────────
        ProfileSectionLabel("Weight")
        Spacer(modifier = Modifier.height(8.dp))
        StepperCard(
            value   = weight.toInt(),
            unit    = "kg",
            color   = AccentAmber,
            onMinus = { if (weight > 40f) onWeight(weight - 1f) },
            onPlus  = { if (weight < 150f) onWeight(weight + 1f) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Start button ──────────────────────────────────────────────────────
        val canStart = name.isNotEmpty() && selectedDate != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (canStart) AccentIndigo.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                .border(
                    1.dp,
                    if (canStart) AccentIndigo.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = canStart) { onDone() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (canStart) "Start Training →" else "Enter your name and date of birth",
                color         = if (canStart) AccentIndigo else Color.White.copy(alpha = 0.2f),
                fontSize      = if (canStart) 16.sp else 13.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = if (canStart) 0.5.sp else 0.sp
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color         = Color.White.copy(alpha = 0.3f),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun StepperCard(value: Int, unit: String, color: Color, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSmBg)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onMinus() },
            contentAlignment = Alignment.Center
        ) { Text("−", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$value", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
            Text(unit, color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) { Text("+", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun GenderChip(text: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else GlassSmBg)
            .border(1.dp, if (selected) color.copy(alpha = 0.5f) else GlassSmBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (text == "Male") "♂" else "♀", color = if (selected) color else Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
            Text(text, color = if (selected) color else Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}