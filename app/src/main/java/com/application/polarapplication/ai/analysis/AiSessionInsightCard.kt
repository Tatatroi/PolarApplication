package com.application.polarapplication.ai.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.polarapplication.BuildConfig
import com.application.polarapplication.model.TrainingSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen  = Color(0xFF4ADE80)
private val AccentAmber  = Color(0xFFFBBF24)
private val AccentRed    = Color(0xFFF87171)
private val CardDark     = Color(0xFF0F0F16)
private val GlassBorder  = Color(0x14FFFFFF)

private val GROQ_API_KEY get() = BuildConfig.GROQ_API_KEY
private const val GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions"
private const val GROQ_MODEL = "llama-3.3-70b-versatile"

// ─────────────────────────────────────────────
// DATA
// ─────────────────────────────────────────────

data class AiInsightResult(
    val estMaxHr:    Int,
    val estAvgHr:    Int,
    val estCalories: Int,
    val estTrimp:    Int,
    val estRpe:      Int,
    val insight:     String
)

// ─────────────────────────────────────────────
// SESIUNI RELEVANTE
// ─────────────────────────────────────────────

private fun relevantSessions(
    sessions:    List<TrainingSessionEntity>,
    workoutType: String
): List<TrainingSessionEntity> {
    val type = workoutType.uppercase()

    // Free activity — combină sesiunile specifice cu echivalentul Bompa
    if (type.startsWith("FREE_")) {
        val activity = type.removePrefix("FREE_")
        val bompaEquivalent = when (activity) {
            "RUNNING", "WALKING", "CYCLING", "SWIMMING" -> "ENDURANCE"
            "SPORT" -> "SPEED"
            else    -> null
        }
        val freeMatches  = sessions.filter { it.type.uppercase() == type }
        val bompaMatches = if (bompaEquivalent  != null)
            sessions.filter { it.type.uppercase() == bompaEquivalent  }
        else emptyList()
        return (freeMatches + bompaMatches).sortedByDescending { it.date }.take(10)
    }

    return sessions.filter { it.type.uppercase() == type }
        .sortedByDescending { it.date }.take(10)
}

// ─────────────────────────────────────────────
// EWMA pe sesiunile relevante
// ─────────────────────────────────────────────

private data class EwmaStats(
    val avgMaxHr:    Int,
    val avgDuration: Int,
    val avgCalories: Int,
    val avgRpe:      Int,
    val count:       Int
)

private fun computeEwmaStats(sessions: List<TrainingSessionEntity>): EwmaStats? {
    if (sessions.isEmpty()) return null
    val alpha = 0.35f

    fun ewma(values: List<Float>): Float {
        var result = values.last()
        for (v in values.reversed()) result = alpha * v + (1f - alpha) * result
        return result
    }

    val maxHrs    = sessions.map { it.maxHeartRate.toFloat() }
    val durations = sessions.map { (it.durationSeconds / 60f) }
    val calories  = sessions.map { it.totalCalories.toFloat() }
    val rpes      = sessions.filter { it.rpe > 0 }.map { it.rpe.toFloat() }

    return EwmaStats(
        avgMaxHr    = ewma(maxHrs).toInt(),
        avgDuration = ewma(durations).toInt(),
        avgCalories = ewma(calories).toInt(),
        avgRpe      = if (rpes.isNotEmpty()) ewma(rpes).toInt() else 6,
        count       = sessions.size
    )
}

// ─────────────────────────────────────────────
// GROQ CALL
// ─────────────────────────────────────────────

private suspend fun callGroqInsight(
    workoutType:  String,
    currentPhase: String,
    stats:        EwmaStats,
    forecast:     SessionForecast?,
    activityType: String = "",
    sessionGoal:  String = "",
    focusArea:    String = "",
    plannedRpe:   Int    = 5
): AiInsightResult = withContext(Dispatchers.IO) {

    val systemPrompt = """
You are a sports science AI. Reply ONLY with valid JSON, no markdown, no extra text.
Analyze the athlete's data and predict next session parameters.
Keep insight to MAX 2 short sentences. Be direct, data-driven, no fluff.
""".trimIndent()

    val userPrompt = """
You are predicting physiological response for a 45-minute $workoutType session.

SESSION DETAILS:
- Type: $workoutType
- Activity: ${activityType.ifEmpty { "not specified" }}
- Goal: ${sessionGoal.ifEmpty { "not specified" }}
- Focus area: ${focusArea.ifEmpty { "not specified" }}
- Planned intensity (RPE): $plannedRpe/10
- Bompa phase: $currentPhase
- Duration: 45 minutes (fixed)

ATHLETE HISTORICAL DATA (EWMA weighted, ${stats.count} sessions):
- Avg Max HR: ${stats.avgMaxHr} bpm
- Avg Calories per session: ${stats.avgCalories} kcal
- Avg RPE: ${stats.avgRpe}/10

RECOVERY STATE:
- CNS from last session: ${forecast?.lastSessionCns ?: 0}%
- Recovery factor: ${((forecast?.recoveryFactor ?: 1f) * 100).toInt()}%
- Physical stress trend: ${forecast?.physicalStress ?: "MODERATE"}

Based on these parameters, predict what the athlete's body will experience during this 45-minute session.
Focus on physiological response, not duration.

Respond ONLY with this JSON:
{
  "estMaxHr": <int, peak bpm expected>,
  "estAvgHr": <int, average bpm for session>,
  "estCalories": <int, kcal for 45 min>,
  "estTrimp": <int, training load score>,
  "estRpe": <int, predicted perceived exertion 1-10>,
  "insight": "<max 2 sentences, physiological focus, data-driven>"
}
""".trimIndent()

    try {
        val conn = (URL(GROQ_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $GROQ_API_KEY")
            connectTimeout = 20_000
            readTimeout    = 30_000
            doOutput       = true
        }

        val msgs = JSONArray().apply {
            put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
            put(JSONObject().apply { put("role", "user");   put("content", userPrompt) })
        }

        val body = JSONObject().apply {
            put("model", GROQ_MODEL)
            put("messages", msgs)
            put("max_tokens", 300)
            put("temperature", 0.3)
        }

        conn.outputStream.write(body.toString().toByteArray(Charsets.UTF_8))
        conn.outputStream.flush()

        val raw = if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        } else {
            return@withContext AiInsightResult(
                estMaxHr    = stats.avgMaxHr,
                estAvgHr    = (stats.avgMaxHr * 0.8f).toInt(),
                estCalories = stats.avgCalories,
                estTrimp    = 50,
                estRpe      = plannedRpe,
                insight     = "Could not connect to AI. Showing statistical estimates."
            )
        }

        val clean = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json  = JSONObject(clean)

        AiInsightResult(
            estMaxHr    = json.optInt("estMaxHr",    stats.avgMaxHr),
            estAvgHr    = json.optInt("estAvgHr",    (stats.avgMaxHr * 0.8f).toInt()),
            estCalories = json.optInt("estCalories", stats.avgCalories),
            estTrimp    = json.optInt("estTrimp",    50),
            estRpe      = json.optInt("estRpe",      plannedRpe),
            insight     = json.optString("insight",  "Stable training pattern detected.")
        )
    } catch (e: Exception) {
    AiInsightResult(
        estMaxHr    = stats.avgMaxHr,
        estAvgHr    = (stats.avgMaxHr * 0.8f).toInt(),
        estCalories = stats.avgCalories,
        estTrimp    = 50,
        estRpe      = plannedRpe,
        insight     = "AI unavailable. Showing statistical estimates."
    )
}
}

// ─────────────────────────────────────────────
// UI CARD
// ─────────────────────────────────────────────

@Composable
fun AiSessionInsightCard(
    sessions:     List<TrainingSessionEntity>,
    workoutType:  String,
    currentPhase: String,
    forecast:     SessionForecast?,
    activityType: String = "",
    sessionGoal:  String = "",
    focusArea:    String = "",
    plannedRpe:   Int    = 5,
    modifier:     Modifier = Modifier
) {
    val relevant = remember(sessions, workoutType) {
        relevantSessions(sessions, workoutType)
    }
    val stats = remember(relevant) { computeEwmaStats(relevant) }

    var aiResult   by remember { mutableStateOf<AiInsightResult?>(null) }
    var isLoading  by remember { mutableStateOf(false) }
    var hasGenerated by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AccentIndigo.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        // ── HEADER ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint     = AccentIndigo,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "AI PREDICTION",
                    color         = Color.White.copy(alpha = 0.3f),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (relevant.isNotEmpty()) {
                    Text(
                        "· ${relevant.size} sessions",
                        color    = AccentIndigo.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }

            // Buton Generate
            if (!hasGenerated || aiResult != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentIndigo.copy(alpha = 0.1f))
                        .border(1.dp, AccentIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isLoading && stats != null) {
                            if (!isLoading && stats != null) {
                                isLoading = true
                                hasGenerated = true
                                aiResult = null
                                scope.launch {
                                    aiResult = callGroqInsight(
                                        workoutType  = workoutType,
                                        currentPhase = currentPhase,
                                        stats        = stats,
                                        forecast     = forecast,
                                        activityType = activityType,
                                        sessionGoal  = sessionGoal,
                                        focusArea    = focusArea,
                                        plannedRpe   = plannedRpe
                                    )
                                    isLoading = false
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (aiResult != null) "Refresh" else "Generate",
                        color      = if (stats != null) AccentIndigo else Color.White.copy(alpha = 0.2f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (stats == null) {
            // Nicio sesiune anterioară
            Text(
                "No previous ${workoutType.lowercase()} sessions found. Complete a session first.",
                color    = Color.White.copy(alpha = 0.25f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            return@Column
        }

        // ── SKELETON sau DATE ─────────────────────────────────────────────
        val displayResult = aiResult

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PredictionBox(
                label    = "MAX HR",
                value    = displayResult?.estMaxHr?.let { "$it" },
                unit     = "bpm",
                color    = AccentRed,
                loading  = isLoading,
                modifier = Modifier.weight(1f)
            )
            PredictionBox(
                label    = "AVG HR",
                value    = displayResult?.estAvgHr?.let { "$it" },
                unit     = "bpm",
                color    = AccentAmber,
                loading  = isLoading,
                modifier = Modifier.weight(1f)
            )
            PredictionBox(
                label    = "TRIMP",
                value    = displayResult?.estTrimp?.let { "$it" },
                unit     = "load",
                color    = AccentIndigo,
                loading  = isLoading,
                modifier = Modifier.weight(1f)
            )
            PredictionBox(
                label    = "CALORIES",
                value    = displayResult?.estCalories?.let { "$it" },
                unit     = "kcal",
                color    = AccentGreen,
                loading  = isLoading,
                modifier = Modifier.weight(1f)
            )
        }

        // ── INSIGHT TEXT ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = displayResult != null,
            enter   = fadeIn(tween(400)),
            exit    = fadeOut()
        ) {
            displayResult?.let {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentIndigo.copy(alpha = 0.05f))
                            .border(1.dp, AccentIndigo.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            it.insight,
                            color      = Color.White.copy(alpha = 0.5f),
                            fontSize   = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        if (!hasGenerated) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap Generate to get AI predictions based on your ${relevant.size} past sessions.",
                color      = Color.White.copy(alpha = 0.18f),
                fontSize   = 9.sp,
                lineHeight = 14.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// PREDICTION BOX — cu skeleton
// ─────────────────────────────────────────────

@Composable
private fun PredictionBox(
    label:   String,
    value:   String?,
    unit:    String,
    color:   Color,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        initialValue   = 0.05f,
        targetValue    = 0.15f,
        animationSpec  = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label          = "alpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color         = Color.White.copy(alpha = 0.2f),
            fontSize      = 7.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (loading) {
            // Shimmer skeleton
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = shimmerAlpha))
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = shimmerAlpha / 2))
            )
        } else if (value != null) {
            Text(
                value,
                color      = color,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 20.sp
            )
            Text(
                unit,
                color    = Color.White.copy(alpha = 0.2f),
                fontSize = 8.sp
            )
        } else {
            // Placeholder înainte de generate
            Text(
                "—",
                color      = Color.White.copy(alpha = 0.1f),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                unit,
                color    = Color.White.copy(alpha = 0.1f),
                fontSize = 8.sp
            )
        }
    }
}