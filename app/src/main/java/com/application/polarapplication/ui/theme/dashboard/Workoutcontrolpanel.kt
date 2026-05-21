package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.ai.model.AthleteVitals

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────
private val BgCard      = Color(0xFF111118)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentRed    = Color(0xFFF87171)
private val AccentAmber  = Color(0xFFFBBF24)
private val AccentBlue   = Color(0xFF60A5FA)

// ─────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────

data class WorkoutSessionConfig(
    val type:         String = "STRENGTH",
    val activityType: String = "",
    val sessionGoal:  String = "",
    val focusArea:    String = "",
    val rpe:          Int    = 5
)

private data class SessionTypeInfo(
    val type:  String,
    val icon:  ImageVector,
    val color: Color
)

private data class ActivityOption(
    val name:  String,
    val icon:  ImageVector
)

private data class Step2Config(
    val activities: List<ActivityOption>,
    val goals:      List<String>,
    val focusAreas: List<String>?  // null = nu se afișează
)

// ─────────────────────────────────────────────
// CONFIGURAȚII PER TIP
// ─────────────────────────────────────────────

private val SESSION_TYPES = listOf(
    SessionTypeInfo("STRENGTH", Icons.Default.FitnessCenter, AccentIndigo),
    SessionTypeInfo("ENDURANCE", Icons.Default.DirectionsRun, AccentGreen),
    SessionTypeInfo("SPEED", Icons.Default.Speed, AccentAmber),
    SessionTypeInfo("RECOVERY", Icons.Default.SelfImprovement, AccentBlue)
)

private fun getStep2Config(type: String): Step2Config = when (type) {
    "STRENGTH" -> Step2Config(
        activities = listOf(
            ActivityOption("Gym",         Icons.Default.FitnessCenter),
            ActivityOption("Bodyweight",  Icons.Default.AccessibilityNew),
            ActivityOption("Kettlebell",  Icons.Default.FitnessCenter),
            ActivityOption("Calisthenics",Icons.Default.SportsGymnastics)
        ),
        goals      = listOf("Build Strength", "Hypertrophy", "Power", "Endurance Strength"),
        focusAreas = listOf("Lower Body", "Upper Body", "Full Body", "Core")
    )
    "ENDURANCE" -> Step2Config(
        activities = listOf(
            ActivityOption("Running",  Icons.Default.DirectionsRun),
            ActivityOption("Cycling",  Icons.Default.DirectionsBike),
            ActivityOption("Swimming", Icons.Default.Pool),
            ActivityOption("Rowing",   Icons.Default.Rowing),
            ActivityOption("Bag Work", Icons.Default.SportsKabaddi)
        ),
        goals      = listOf("Improve Endurance", "Fat Burn", "Build Base", "Race Prep"),
        focusAreas = null
    )
    "SPEED" -> Step2Config(
        activities = listOf(
            ActivityOption("Sprints",       Icons.Default.Speed),
            ActivityOption("Martial Arts",  Icons.Default.SportsMartialArts),
            ActivityOption("Boxing",        Icons.Default.SportsKabaddi),
            ActivityOption("Intervals",     Icons.Default.Timer),
            ActivityOption("Agility",       Icons.Default.DirectionsRun)
        ),
        goals      = listOf("Improve Speed", "Explosive Power", "Agility", "Fight Conditioning"),
        focusAreas = null
    )
    "RECOVERY" -> Step2Config(
        activities = listOf(
            ActivityOption("Walking",    Icons.Default.DirectionsWalk),
            ActivityOption("Yoga",       Icons.Default.SelfImprovement),
            ActivityOption("Stretching", Icons.Default.AccessibilityNew),
            ActivityOption("Light Swim", Icons.Default.Pool)
        ),
        goals      = listOf("Active Recovery", "Mobility", "Flexibility", "Stress Relief"),
        focusAreas = null
    )
    else -> Step2Config(
        activities = emptyList(),
        goals      = emptyList(),
        focusAreas = null
    )
}

private fun typeColor(type: String) = when (type) {
    "STRENGTH" -> AccentIndigo
    "ENDURANCE" -> AccentGreen
    "SPEED" -> AccentAmber
    "RECOVERY" -> AccentBlue
    else -> Color(0xFF666677)
}

// ─────────────────────────────────────────────
// MAIN COMPOSABLE
// ─────────────────────────────────────────────

@Composable
fun WorkoutControlPanel(
    isActive:          Boolean,
    vitals:            AthleteVitals,
    onStart:           (WorkoutSessionConfig) -> Unit,
    onStop:            (String) -> Unit,
    onMaximizeWorkout: () -> Unit,
    onTypeSelected:    (String) -> Unit = {}
) {
    var config by remember {
        mutableStateOf(WorkoutSessionConfig())
    }
    var activeConfig by remember { mutableStateOf(WorkoutSessionConfig()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(
                1.dp,
                if (isActive) AccentAmber.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        if (!isActive) {
            PrepareWorkoutContent(
                vitals    = vitals,
                config    = config,
                onConfigChange = { config = it; onTypeSelected(it.type) },
                onStart   = {
                    activeConfig = config
                    onStart(config)
                }
            )
        } else {
            ActiveWorkoutContent(
                vitals             = vitals,
                config             = activeConfig,
                onMaximizeWorkout  = onMaximizeWorkout,
                onStop             = { onStop(activeConfig.type) }
            )
        }
    }
}

// ─────────────────────────────────────────────
// PREPARE WORKOUT
// ─────────────────────────────────────────────

@Composable
private fun PrepareWorkoutContent(
    vitals:         AthleteVitals,
    config:         WorkoutSessionConfig,
    onConfigChange: (WorkoutSessionConfig) -> Unit,
    onStart:        () -> Unit
) {
    val step2 = getStep2Config(config.type)
    val color = typeColor(config.type)

    // Label header
    Text(
        "PREPARE WORKOUT",
        color         = Color.White.copy(alpha = 0.25f),
        fontSize      = 9.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )

    // CNS recomandare
    val (recText, recColor) = when {
        vitals.cnsScore >= 80 -> "CNS rested — recommended: STRENGTH / SPEED" to AccentGreen
        vitals.cnsScore >= 50 -> "CNS normal — recommended: ENDURANCE" to AccentAmber
        vitals.cnsScore > 0   -> "CNS fatigued — recommended: RECOVERY" to AccentRed
        else                  -> "Connect sensor for CNS analysis" to Color.White.copy(alpha = 0.3f)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(recText, color = recColor, fontSize = 11.sp, lineHeight = 16.sp)
    Spacer(modifier = Modifier.height(14.dp))

    // ── STEP 1: Tip antrenament ───────────────────────────────────────────────
    SectionLabel("1. Select session type")
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SESSION_TYPES.forEach { info ->
            val isSelected = config.type == info.type
            SessionTypeChip(
                info       = info,
                isSelected = isSelected,
                modifier   = Modifier.weight(1f),
                onClick    = {
                    onConfigChange(WorkoutSessionConfig(
                        type         = info.type,
                        activityType = "",
                        sessionGoal  = "",
                        focusArea    = ""
                    ))
                }
            )
        }
    }

    // ── STEP 2: Opțiuni specifice ─────────────────────────────────────────────
    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "2. ${config.type.lowercase().replaceFirstChar { it.uppercase() }} Options",
                color      = color,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Activitate
            if (step2.activities.isNotEmpty()) {
                DropdownRow(
                    label       = "Activity",
                    icon        = Icons.Default.Category,
                    color       = color,
                    selected    = config.activityType.ifEmpty { "Select activity" },
                    options     = step2.activities.map { it.name },
                    onSelect    = { onConfigChange(config.copy(activityType = it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Goal
            if (step2.goals.isNotEmpty()) {
                DropdownRow(
                    label    = "Workout Goal",
                    icon     = Icons.Default.TrackChanges,
                    color    = color,
                    selected = config.sessionGoal.ifEmpty { "Select goal" },
                    options  = step2.goals,
                    onSelect = { onConfigChange(config.copy(sessionGoal = it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Focus Area (doar pentru Strength)
            if (step2.focusAreas != null) {
                DropdownRow(
                    label    = "Focus Area",
                    icon     = Icons.Default.MyLocation,
                    color    = color,
                    selected = config.focusArea.ifEmpty { "Select area" },
                    options  = step2.focusAreas,
                    onSelect = { onConfigChange(config.copy(focusArea = it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // RPE Slider
            RpeSlider(
                rpe    = config.rpe,
                color  = color,
                onChange = { onConfigChange(config.copy(rpe = it)) }
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (vitals.heartRate == 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AccentAmber.copy(alpha = 0.07f))
                .border(1.dp, AccentAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = AccentAmber, modifier = Modifier.size(14.dp))
            Text(
                "Waiting for heart rate signal...",
                color = AccentAmber.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Buton Start
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (vitals.heartRate > 0) color.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
            .clickable { onStart() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                "START WORKOUT",
                color      = color,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// ACTIVE WORKOUT
// ─────────────────────────────────────────────

@Composable
private fun ActiveWorkoutContent(
    vitals:            AthleteVitals,
    config:            WorkoutSessionConfig,
    onMaximizeWorkout: () -> Unit,
    onStop:            () -> Unit
) {
    val color = typeColor(config.type)

    Text(
        "ACTIVE SESSION",
        color         = AccentAmber,
        fontSize      = 9.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(config.type, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        if (config.activityType.isNotEmpty()) {
            Text("·", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp)
            Text(config.activityType, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }

    if (config.sessionGoal.isNotEmpty()) {
        Text(config.sessionGoal, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Metrici live
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("TRIMP",    "%.1f".format(vitals.trimpScore), AccentIndigo),
            Triple("CALORIES", "${vitals.calories}",             AccentAmber),
            Triple("CNS",      "${vitals.cnsScore}",             AccentGreen)
        ).forEach { (label, value, metricColor) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(metricColor.copy(alpha = 0.07f))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label, color = Color.White.copy(alpha = 0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(value, color = metricColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentIndigo.copy(alpha = 0.1f))
            .border(1.dp, AccentIndigo.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onMaximizeWorkout() },
        contentAlignment = Alignment.Center
    ) {
        Text("Live Details →", color = AccentIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.1f))
            .border(1.dp, AccentRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onStop() },
        contentAlignment = Alignment.Center
    ) {
        Text("STOP & SAVE", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

// ─────────────────────────────────────────────
// COMPONENTE HELPER
// ─────────────────────────────────────────────

@Composable
private fun SessionTypeChip(
    info:       SessionTypeInfo,
    isSelected: Boolean,
    modifier:   Modifier,
    onClick:    () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) info.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (isSelected) info.color.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            info.icon,
            null,
            tint     = if (isSelected) info.color else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            info.type.take(3),  // STR, END, SPD, REC
            color      = if (isSelected) info.color else Color.White.copy(alpha = 0.25f),
            fontSize   = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun DropdownRow(
    label:    String,
    icon:     ImageVector,
    color:    Color,
    selected: String,
    options:  List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                Text(
                    selected,
                    color      = if (selected.startsWith("Select")) Color.White.copy(alpha = 0.25f) else Color.White,
                    fontSize   = 13.sp,
                    modifier   = Modifier.weight(1f)
                )
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text    = { Text(option) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun RpeSlider(
    rpe:      Int,
    color:    Color,
    onChange: (Int) -> Unit
) {
    val rpeLabel = when {
        rpe <= 2 -> "Very Easy"
        rpe <= 4 -> "Easy"
        rpe <= 6 -> "Moderate"
        rpe <= 8 -> "Hard"
        else     -> "Maximum"
    }
    val rpeColor = when {
        rpe <= 3 -> AccentGreen
        rpe <= 6 -> AccentAmber
        rpe <= 8 -> AccentRed.copy(alpha = 0.7f)
        else     -> AccentRed
    }

    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Planned Intensity (RPE)", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$rpe/10", color = rpeColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(rpeLabel, color = rpeColor.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
        Slider(
            value         = rpe.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = 1f..10f,
            steps         = 8,
            colors        = SliderDefaults.colors(
                thumbColor       = rpeColor,
                activeTrackColor = rpeColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color         = Color.White.copy(alpha = 0.4f),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 0.3.sp
    )
}