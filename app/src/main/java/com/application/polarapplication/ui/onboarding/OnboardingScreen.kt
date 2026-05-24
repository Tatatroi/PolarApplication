package com.application.polarapplication.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.R
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgDark       = Color(0xFF080808)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentAmber  = Color(0xFFFBBF24)
private val GlassBg      = Color(0x0AFFFFFF)
private val GlassBorder  = Color(0x14FFFFFF)
private val GlassSmBg    = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)

// ─────────────────────────────────────────────
// MAIN ONBOARDING
// ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (name: String, age: Int, gender: String, height: Int, weight: Float) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope      = rememberCoroutineScope()

    // Profile state
    var name   by remember { mutableStateOf("") }
    var age    by remember { mutableStateOf(25) }
    var gender by remember { mutableStateOf("Male") }
    var height by remember { mutableStateOf(175) }
    var weight by remember { mutableStateOf(75f) }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false  // navigare doar cu buton
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> ProfilePage(
                    name     = name,
                    age      = age,
                    gender   = gender,
                    height   = height,
                    weight   = weight,
                    onName   = { name = it },
                    onAge    = { age = it },
                    onGender = { gender = it },
                    onHeight = { height = it },
                    onWeight = { weight = it },
                    onDone   = { onComplete(name, age, gender, height, weight) }
                )
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
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
                        .background(if (isActive) AccentIndigo else Color.White.copy(alpha = 0.2f))
                )
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

        // Imagine full-screen
        Image(
            painter            = painterResource(id = R.drawable.firstimageintro),
            contentDescription = "AthleteIQ",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Gradient overlay — jos spre sus, ca textul să fie lizibil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to BgDark.copy(alpha = 0.5f),
                            0.35f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to BgDark
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            // Top — logo + tagline
            Column(
                modifier            = Modifier.padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo text
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Athlete",
                        color      = Color.White,
                        fontSize   = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "IQ",
                        color      = AccentIndigo,
                        fontSize   = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Structured training. Measurable progress.",
                    color     = Color.White.copy(alpha = 0.6f),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Strength · Speed · Endurance
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Build", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    Text("Strength,", color = AccentIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Speed", color = AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("&", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    Text("Endurance.", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom — feature cards + buton
            Column(
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp),
                modifier              = Modifier.padding(bottom = 72.dp)
            ) {
                // 3 feature cards
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureCard("💪", "Strength",  "Build power\nand resilience.",  AccentIndigo, Modifier.weight(1f))
                    FeatureCard("⚡", "Speed",     "Increase explosiveness\nand agility.", AccentAmber,  Modifier.weight(1f))
                    FeatureCard("❤️", "Endurance", "Improve stamina\nand performance.", AccentGreen,  Modifier.weight(1f))
                }

                // Buton Get Started
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentIndigo.copy(alpha = 0.15f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Get Started →",
                        color      = AccentIndigo,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(icon: String, title: String, desc: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(desc, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

// ─────────────────────────────────────────────
// PAGE 2 — PROFILE
// ─────────────────────────────────────────────

@Composable
private fun ProfilePage(
    name:     String,
    age:      Int,
    gender:   String,
    height:   Int,
    weight:   Float,
    onName:   (String) -> Unit,
    onAge:    (Int) -> Unit,
    onGender: (String) -> Unit,
    onHeight: (Int) -> Unit,
    onWeight: (Float) -> Unit,
    onDone:   () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // Header
        Text(
            "Tell us about yourself",
            color      = Color.White,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "We use this to personalize your training and calculate accurate metrics.",
            color      = Color.White.copy(alpha = 0.4f),
            fontSize   = 13.sp,
            lineHeight = 19.sp,
            modifier   = Modifier.padding(top = 6.dp, bottom = 32.dp)
        )

        // Name
        ProfileSectionLabel("Your Name")
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value         = name,
            onValueChange = onName,
            modifier      = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSmBg)
                .border(1.dp, if (name.isNotEmpty()) AccentIndigo.copy(alpha = 0.4f) else GlassSmBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle     = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush   = SolidColor(AccentIndigo),
            singleLine    = true,
            decorationBox = { inner ->
                if (name.isEmpty()) Text("e.g. Alex Johnson", color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                inner()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Age
        ProfileSectionLabel("Age")
        Spacer(modifier = Modifier.height(8.dp))
        StepperCard(
            value    = age,
            unit     = "years",
            color    = AccentIndigo,
            onMinus  = { if (age > 14) onAge(age - 1) },
            onPlus   = { if (age < 80) onAge(age + 1) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender
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

        // Height
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

        // Weight
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

        // Start button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (name.isNotEmpty()) AccentIndigo.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.03f)
                )
                .border(
                    1.dp,
                    if (name.isNotEmpty()) AccentIndigo.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = name.isNotEmpty()) { onDone() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Start Training →",
                color      = if (name.isNotEmpty()) AccentIndigo else Color.White.copy(alpha = 0.2f),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// HELPER COMPOSABLES
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
        // Minus
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onMinus() },
            contentAlignment = Alignment.Center
        ) {
            Text("−", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Value
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$value",
                color      = Color.White,
                fontSize   = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp
            )
            Text(
                unit,
                color    = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Plus
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(if (text == "Male") "♂" else "♀", color = if (selected) color else Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
            Text(text, color = if (selected) color else Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}