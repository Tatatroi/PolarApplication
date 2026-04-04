package com.application.polarapplication.ai.chatbot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.polarapplication.BuildConfig
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.ui.theme.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
// ─────────────────────────────────────────────
// GEMINI CONFIG
// ─────────────────────────────────────────────
private val GROQ_API_KEY get() = BuildConfig.GROQ_API_KEY
private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val GROQ_MODEL = "llama-3.3-70b-versatile"

// ─────────────────────────────────────────────
// CULORI
// ─────────────────────────────────────────────
private val BgDark = Color(0xFF080808)
private val GlassBg = Color(0x0AFFFFFF)
private val GlassBorder = Color(0x14FFFFFF)
private val GlassSmBg = Color(0x0DFFFFFF)
private val GlassSmBorder = Color(0x17FFFFFF)
private val AccentIndigo = Color(0xFF818CF8)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentRed = Color(0xFFF87171)
private val AccentAmber = Color(0xFFFBBF24)
private val AccentBlue = Color(0xFF60A5FA)

// ─────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────

// Răspunsul sesiunii de onboarding
data class SessionSetup(
    val duration: String = "45 min",
    val goal: String = "",
    val location: String = "",
    val equipment: List<String> = emptyList()
)

// Mesaj în conversație
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Ai(val text: String, val plan: WorkoutPlan? = null) : ChatMessage()
}

// Planul de antrenament parsit din JSON
data class WorkoutPlan(
    val title: String,
    val type: String,
    val warning: String?,
    val bompaContext: String?,
    val steps: List<WorkoutStep>,
    val safetyReminder: String?
)

data class WorkoutStep(
    val number: Int,
    val title: String,
    val duration: String?,
    val description: String,
    val tags: List<String>
)

// ─────────────────────────────────────────────
// PARSARE JSON RĂSPUNS GEMINI
// ─────────────────────────────────────────────
private fun parseWorkoutPlan(raw: String): WorkoutPlan? {
    return try {
        // Extragem JSON-ul din răspuns (poate conține text înainte/după)
        val jsonStart = raw.indexOf("{")
        val jsonEnd = raw.lastIndexOf("}") + 1
        if (jsonStart == -1 || jsonEnd == 0) return null
        val jsonStr = raw.substring(jsonStart, jsonEnd)

        val json = JSONObject(jsonStr)
        val planObj = json.optJSONObject("plan") ?: return null
        val stepsArr = planObj.optJSONArray("steps") ?: JSONArray()

        val steps = (0 until stepsArr.length()).map { i ->
            val s = stepsArr.getJSONObject(i)
            WorkoutStep(
                number = s.optInt("number", i + 1),
                title = s.optString("title", "Pas ${i + 1}"),
                duration = s.optString("duration").takeIf { it.isNotBlank() && it != "null" },
                description = s.optString("description", ""),
                tags = (s.optJSONArray("tags") ?: JSONArray()).let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                }
            )
        }

        WorkoutPlan(
            title = planObj.optString("title", "Plan antrenament"),
            type = planObj.optString("type", "GENERAL"),
            warning = json.optString("warning").takeIf { it.isNotBlank() && it != "null" },
            bompaContext = json.optString("bompaContext").takeIf { it.isNotBlank() && it != "null" },
            steps = steps,
            safetyReminder = json.optString("safetyReminder").takeIf { it.isNotBlank() && it != "null" }
        )
    } catch (e: Exception) {
        null
    }
}

// ─────────────────────────────────────────────
// SYSTEM PROMPT
// ─────────────────────────────────────────────
private fun buildSystemPrompt(
    setup: SessionSetup,
    cnsScore: Int,
    heartRate: Int,
    phaseName: String,
    weekNum: Int,
    totalWeeks: Int,
    todayWorkout: String,
    daysToCompetition: Long,
    recentSessions: List<TrainingSessionEntity>,
    weeklyTrimp: Double
): String {
    val cnsStatus = when {
        cnsScore >= 70 -> "odihnit — poate antrena intens"
        cnsScore >= 50 -> "normal — antrenament standard recomandat"
        cnsScore > 0 -> "obosit — recuperare prioritară"
        else -> "necunoscut (senzor deconectat)"
    }

    val sessionsSummary = if (recentSessions.isEmpty()) {
        "Nicio sesiune salvată."
    } else {
        val fmt = SimpleDateFormat("dd MMM", Locale("ro"))
        recentSessions.take(5).joinToString("\n") { s ->
            "- ${fmt.format(Date(s.date))}: ${s.type}, " +
                "TRIMP ${"%.1f".format(s.finalTrimp)}, HR avg ${s.avgHeartRate} bpm"
        }
    }

    val equipmentStr = if (setup.equipment.isEmpty()) {
        "nespecificat"
    } else {
        setup.equipment.joinToString(", ")
    }

    return """
Ești un asistent virtual sportiv de elită, specializat în periodizarea Bompa.
Răspunzi în ACEEAȘI LIMBĂ în care ți se pune întrebarea (română sau engleză).
Ești motivant, precis și orientat spre siguranța cardiacă.

SESIUNEA CONFIGURATĂ DE UTILIZATOR:
- Durată disponibilă: ${setup.duration}
- Obiectiv azi: ${setup.goal}
- Locație: ${setup.location}
- Echipament disponibil: $equipmentStr

DATE BIOMETRICE ÎN TIMP REAL:
- CNS Score: $cnsScore/100 ($cnsStatus)
- Puls live: ${if (heartRate > 0) "$heartRate BPM" else "senzor deconectat"}
- Faza Bompa: $phaseName (săptămâna $weekNum din $totalWeeks)
- Antrenament planificat azi de Bompa: $todayWorkout
- Zile până la competiție: $daysToCompetition
- TRIMP săptămâna asta: ${"%.1f".format(weeklyTrimp)}

ULTIMELE SESIUNI:
$sessionsSummary

REGULI STRICTE:
1. SIGURANȚĂ CARDIACĂ — prioritate absolută:
   - Puls > 180 BPM → warning imediat, recomandă oprire
   - Puls 160-180 BPM → atenționează, ajustează intensitatea
   - CNS < 40 → recomandă obligatoriu recuperare

2. ADAPTARE LA ECHIPAMENT:
   - Recomandă DOAR exerciții fezabile cu echipamentul declarat
   - Dacă e la pistă → exerciții de alergare, pliometrie
   - Dacă e la sală forță → exerciții cu bare, gantere
   - Dacă e la sac de box → combinații, anduranță specifică
   - Dacă e acasă fără echipament → bodyweight

3. TIPURI DE ANTRENAMENT — generează MINIM 6-8 exerciții per sesiune:
   - Forță/Strength → 
     * Încălzire: 2-3 exerciții mobilitate/activare
     * Bloc principal: 4-5 exerciții compuse (Squat, Deadlift, Bench, Row, OHP)
     * Finalizare: 2-3 exerciții izolație
     * Seturi: 4-5×5 la 80-85% 1RM, pauze 3 min
   - Viteză/Speed →
     * Încălzire dinamică: 3-4 exerciții activare
     * Bloc principal: 5-6 exerciții sprint/pliometrie variate
     * Condiționare: 2-3 exerciții suport
   - Rezistență/Endurance →
     * Faze clare: încălzire, bloc aerob, intervale, revenire
     * Minim 4 faze distincte cu durate clare
   - Recuperare/Recovery →
     * 6-8 exerciții mobilitate și stretching cu durată per exercițiu

4. STRUCTURA UNUI PAS în JSON trebuie să conțină exerciții CONCRETE:
   - Nu "exerciții compuse" ci "Squat 4×5, Deadlift 4×4, Bench Press 4×5"
   - Fiecare pas are cel puțin 2-3 exerciții specifice cu seturi/repetări
   - Adaptează la echipamentul declarat

5. PLANUL BOMPA:
   - Conectează recomandarea la faza curentă ($phaseName)
   - Dacă CNS < 50 și planul zice antrenament intens, sugerează modificare
   - Respectă durata configurată: ${setup.duration}

6. FORMAT RĂSPUNS — OBLIGATORIU JSON EXACT, fără text în afara JSON:
{
  "message": "1-2 propoziții introductive bazate pe datele biometrice",
  "warning": "string dacă pulsul/CNS necesită atenție, altfel null",
  "bompaContext": "string explicând legătura cu faza Bompa curentă, altfel null",
  "plan": {
    "title": "Titlul planului",
    "type": "STRENGTH | SPEED | ENDURANCE | RECOVERY",
    "steps": [
      {
        "number": 1,
        "title": "Numele pasului",
        "duration": "X min sau null",
        "description": "Ce face exact, cu seturi/repetări/distanțe concrete",
        "tags": ["tag concret", "altul"]
      }
    ]
  },
  "safetyReminder": "reminder cardiac dacă e cazul, altfel null"
}
    """.trimIndent()
}

// ─────────────────────────────────────────────
// GEMINI API CALL
// ─────────────────────────────────────────────
private suspend fun callGeminiApi(
    messages: List<ChatMessage>,
    systemPrompt: String
): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(GROQ_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $GROQ_API_KEY")
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.doOutput = true

        val msgs = JSONArray()

        // System prompt
        msgs.put(
            JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            }
        )

        // Istoricul conversației
        messages.forEach { msg ->
            msgs.put(
                JSONObject().apply {
                    put(
                        "role",
                        when (msg) {
                            is ChatMessage.User -> "user"
                            is ChatMessage.Ai -> "assistant"
                        }
                    )
                    put(
                        "content",
                        when (msg) {
                            is ChatMessage.User -> msg.text
                            is ChatMessage.Ai -> msg.text
                        }
                    )
                }
            )
        }

        val body = JSONObject().apply {
            put("model", GROQ_MODEL)
            put("messages", msgs)
            put("max_tokens", 2048)
            put("temperature", 0.7)
        }

        conn.outputStream.write(body.toString().toByteArray(Charsets.UTF_8))
        conn.outputStream.flush()

        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val resp = conn.inputStream.bufferedReader().readText()
            JSONObject(resp)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
            when (conn.responseCode) {
                401 -> """{"message":"API key invalid. Verifică cheia Groq.","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
                429 -> """{"message":"Prea multe cereri. Încearcă în câteva secunde.","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
                else -> """{"message":"Eroare (${conn.responseCode}).","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
            }
        }
    } catch (e: java.net.ConnectException) {
        """{"message":"Nu pot conecta la internet.","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
    } catch (e: java.net.SocketTimeoutException) {
        """{"message":"Timeout. Încearcă din nou.","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
    } catch (e: Exception) {
        """{"message":"Eroare: ${e.message}","warning":null,"bompaContext":null,"plan":null,"safetyReminder":null}"""
    }
}

// ─────────────────────────────────────────────
// ECRAN PRINCIPAL — gestionează cele 3 stări
// ─────────────────────────────────────────────

private enum class ChatScreen { ONBOARDING, SUMMARY, CHAT }

@Composable
fun ChatBotScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val compDate by viewModel.competitionDate.collectAsState()
    val planStartDate by viewModel.planStartDate.collectAsState()
    val today = remember { LocalDate.now() }

    val planner = remember { com.application.polarapplication.ai.planning.TrainingPlanner() }
    val effectiveStart = planStartDate ?: today
    val effectiveComp = compDate ?: today.plusWeeks(24)
    val plan = remember(effectiveStart, effectiveComp) {
        planner.generatePlan(effectiveComp, effectiveStart)
    }
    val totalWeeks = plan.mesoCycles.sumOf { it.microCycle.size }
    val currentMeso = plan.mesoCycles.firstOrNull { meso ->
        meso.microCycle.any { !it.startDate.isAfter(today) && !it.endDate.isBefore(today) }
    } ?: plan.mesoCycles.firstOrNull()
    val currentMicro = currentMeso?.microCycle?.firstOrNull {
        !it.startDate.isAfter(today) && !it.endDate.isBefore(today)
    } ?: currentMeso?.microCycle?.firstOrNull()
    val todayWorkout = run {
        val dayIndex = (today.dayOfWeek.value - 1).coerceIn(0, 6)
        currentMicro?.workouts?.getOrNull(dayIndex)?.name ?: "REST"
    }
    val currentWeekNum = plan.mesoCycles.flatMap { it.microCycle }
        .indexOfFirst { it.startDate == currentMicro?.startDate }.plus(1).coerceAtLeast(1)
    val daysToComp = java.time.temporal.ChronoUnit.DAYS.between(today, effectiveComp)
    val weeklyTrimp = remember(allSessions) {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        allSessions.filter { it.date >= weekAgo }.sumOf { it.finalTrimp }
    }

    val savedMessages by viewModel.chatMessages.collectAsState()
    val savedSetup by viewModel.chatSetup.collectAsState()
    var messages by remember { mutableStateOf(savedMessages) }
    var setup by remember { mutableStateOf(savedSetup ?: SessionSetup()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var currentScreen by remember {
        mutableStateOf(
            if (savedSetup != null) {
                ChatScreen.CHAT
            } else {
                ChatScreen.ONBOARDING
            }
        )
    }

    val systemPrompt = remember(setup, uiState.vitals, allSessions, currentMeso) {
        buildSystemPrompt(
            setup = setup,
            cnsScore = uiState.vitals.cnsScore,
            heartRate = uiState.vitals.heartRate,
            phaseName = currentMeso?.phase ?: "General",
            weekNum = currentWeekNum,
            totalWeeks = totalWeeks,
            todayWorkout = todayWorkout,
            daysToCompetition = daysToComp,
            recentSessions = allSessions.take(10),
            weeklyTrimp = weeklyTrimp
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return
        val userMsg = ChatMessage.User(text.trim())
        messages = messages + userMsg
        inputText = ""
        isLoading = true
        scope.launch {
            val rawResponse = callGeminiApi(messages, systemPrompt)

// Curățăm răspunsul de markdown code blocks dacă Gemini le adaugă
            val cleanResponse = rawResponse
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val plan = parseWorkoutPlan(cleanResponse)

            val displayText = try {
                val json = JSONObject(cleanResponse)
                json.optString("message").takeIf { it.isNotBlank() } ?: cleanResponse
            } catch (e: Exception) {
                // Nu e JSON — afișează textul direct
                cleanResponse
            }

            messages = messages + ChatMessage.Ai(text = displayText, plan = plan)
            viewModel.saveChatMessages(messages)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top bar persistent
        ChatTopBar(
            cnsScore = uiState.vitals.cnsScore,
            phaseName = currentMeso?.phase ?: "—",
            weekNum = currentWeekNum,
            totalWeeks = totalWeeks,
            todayWorkout = todayWorkout,
            daysToComp = daysToComp,
            showContext = currentScreen == ChatScreen.CHAT,
            onNewSession = {
                viewModel.clearChat()
                currentScreen = ChatScreen.ONBOARDING
                messages = emptyList()
                setup = SessionSetup()
            }
        )

        when (currentScreen) {
            ChatScreen.ONBOARDING -> OnboardingScreen(
                initialSetup = setup,
                onComplete = { s -> setup = s; viewModel.saveChatSetup(s); currentScreen = ChatScreen.SUMMARY }
            )
            ChatScreen.SUMMARY -> SummaryScreen(
                setup = setup,
                cnsScore = uiState.vitals.cnsScore,
                onEdit = { currentScreen = ChatScreen.ONBOARDING },
                onStart = {
                    currentScreen = ChatScreen.CHAT
                    val firstMsg = buildFirstMessage(setup)
                    sendMessage(firstMsg)
                }
            )
            ChatScreen.CHAT -> ChatScreen(
                messages = messages,
                listState = listState,
                inputText = inputText,
                isLoading = isLoading,
                onInputChange = { inputText = it },
                onSend = { sendMessage(inputText) }
            )
        }
    }
}

private fun buildFirstMessage(setup: SessionSetup): String {
    val eq = if (setup.equipment.isEmpty()) {
        "echipament standard"
    } else {
        setup.equipment.joinToString(", ")
    }
    return "Vreau să fac ${setup.goal} timp de ${setup.duration}. " +
        "Sunt la: ${setup.location}. Echipament disponibil: $eq. " +
        "Creează-mi un plan de antrenament complet."
}

// ─────────────────────────────────────────────
// ONBOARDING
// ─────────────────────────────────────────────

@Composable
private fun OnboardingScreen(
    initialSetup: SessionSetup,
    onComplete: (SessionSetup) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(initialSetup.duration) }
    var goal by remember { mutableStateOf(initialSetup.goal) }
    var location by remember { mutableStateOf(initialSetup.location) }
    var equipment by remember { mutableStateOf(initialSetup.equipment.toMutableList()) }

    val totalSteps = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { i ->
                val isActive = i == step
                val isDone = i < step
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isActive) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isDone -> AccentGreen
                                isActive -> AccentIndigo
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        )
                )
                if (i < totalSteps - 1) Spacer(modifier = Modifier.width(5.dp))
            }
        }

        when (step) {
            0 -> DurationStep(selected = duration, onSelect = { duration = it })
            1 -> GoalStep(selected = goal, onSelect = { goal = it })
            2 -> LocationStep(selected = location, onSelect = { location = it })
            3 -> EquipmentStep(
                selected = equipment,
                onToggle = { item ->
                    if (equipment.contains(item)) {
                        equipment.remove(item)
                    } else {
                        equipment.add(item)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) { Text("← Înapoi") }
            }
            Button(
                onClick = {
                    if (step < totalSteps - 1) {
                        step++
                    } else {
                        onComplete(SessionSetup(duration, goal, location, equipment.toList()))
                    }
                },
                modifier = Modifier.weight(if (step > 0) 2f else 1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(13.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
            ) {
                Text(
                    if (step < totalSteps - 1) "Continuă →" else "Generează planul →",
                    color = AccentIndigo,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepHeader(questionNum: Int, total: Int, title: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "ÎNTREBAREA $questionNum DIN $total",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DurationStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("20 min", "30 min", "45 min", "60 min", "90 min")
    Column {
        StepHeader(1, 4, "Cât timp ai disponibil?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { opt ->
                        SelectableChip(
                            text = opt,
                            selected = selected == opt,
                            onClick = { onSelect(opt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // fill empty slots
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("💪", "Forță", "Greutăți, rezistență musculară"),
        Triple("⚡", "Viteză", "Sprinturi, explozivitate"),
        Triple("🏃", "Rezistență", "Cardio, anduranță"),
        Triple("💧", "Recuperare", "Mobilitate, stretching")
    )
    Column {
        StepHeader(2, 4, "Ce vrei să antrenezi azi?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (icon, text, sub) ->
                SelectableCard(
                    icon = icon,
                    text = text,
                    subtext = sub,
                    selected = selected == text,
                    onClick = { onSelect(text) }
                )
            }
        }
    }
}

@Composable
private fun LocationStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("🏋️", "Sală de forță", "Echipament complet"),
        Triple("🏃", "Pistă / Afară", "Alergare, sprinturi"),
        Triple("🥊", "Sală box / MMA", "Saci, mănuși, saltele"),
        Triple("🏠", "Acasă", "Fără echipament special")
    )
    Column {
        StepHeader(3, 4, "Unde te antrenezi?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (icon, text, sub) ->
                SelectableCard(
                    icon = icon,
                    text = text,
                    subtext = sub,
                    selected = selected == text,
                    onClick = { onSelect(text) }
                )
            }
        }
    }
}

@Composable
private fun EquipmentStep(
    selected: MutableList<String>,
    onToggle: (String) -> Unit
) {
    var refresh by remember { mutableStateOf(0) }
    val options = listOf(
        "Gantere",
        "Bara + discuri",
        "Aparat cardio",
        "Sac de box",
        "Cauciucuri",
        "Saltea",
        "Corzi",
        "Nimic special"
    )
    Column {
        StepHeader(4, 4, "Ce echipament ai la dispoziție?")
        Text(
            text = "Poți selecta mai multe",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // trigger recompose la toggle
        key(refresh) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { opt ->
                            SelectableChip(
                                text = opt,
                                selected = selected.contains(opt),
                                onClick = { onToggle(opt); refresh++ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableCard(
    icon: String,
    text: String,
    subtext: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentIndigo.copy(alpha = 0.12f) else GlassBg)
            .border(
                1.dp,
                if (selected) AccentIndigo.copy(alpha = 0.5f) else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 22.sp)
        Column {
            Text(
                text = text,
                color = if (selected) AccentIndigo else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                color = if (selected) AccentIndigo.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AccentIndigo.copy(alpha = 0.2f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AccentIndigo.copy(alpha = 0.15f) else GlassSmBg)
            .border(
                1.dp,
                if (selected) AccentIndigo.copy(alpha = 0.5f) else GlassSmBorder,
                RoundedCornerShape(11.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AccentIndigo else Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────
// SUMMARY
// ─────────────────────────────────────────────

@Composable
private fun SummaryScreen(
    setup: SessionSetup,
    cnsScore: Int,
    onEdit: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Rezumat sesiune",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            "Verifică înainte să generezi planul",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryRow("Durată", setup.duration)
            SummaryRow("Obiectiv", setup.goal)
            SummaryRow("Locație", setup.location)
            SummaryRow(
                "Echipament",
                if (setup.equipment.isEmpty()) {
                    "Nespecificat"
                } else {
                    setup.equipment.joinToString(", ")
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CNS info
        val cnsColor = when {
            cnsScore >= 70 -> AccentGreen
            cnsScore >= 50 -> AccentAmber
            cnsScore > 0 -> AccentRed
            else -> Color.White.copy(alpha = 0.3f)
        }
        val cnsText = when {
            cnsScore >= 70 -> "CNS $cnsScore — odihnit, poți antrena intens"
            cnsScore >= 50 -> "CNS $cnsScore — normal, antrenament standard"
            cnsScore > 0 -> "CNS $cnsScore — obosit, ia în calcul recuperarea"
            else -> "Senzor deconectat — CNS necunoscut"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cnsColor.copy(alpha = 0.07f))
                .border(0.5.dp, cnsColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(cnsColor))
            Text(cnsText, color = cnsColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("← Modifică răspunsurile", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
        ) {
            Text(
                "Generează planul →",
                color = AccentIndigo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────
// CHAT
// ─────────────────────────────────────────────

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                when (msg) {
                    is ChatMessage.User -> UserBubble(msg.text)
                    is ChatMessage.Ai -> AiBubble(text = msg.text, plan = msg.plan)
                }
            }
            if (isLoading) item { TypingIndicator() }
        }

        ChatInput(
            value = inputText,
            onValueChange = onInputChange,
            onSend = onSend,
            isLoading = isLoading
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(AccentIndigo.copy(alpha = 0.18f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.35f), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .padding(10.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(GlassSmBg)
                .border(1.dp, GlassSmBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("Tu", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiBubble(text: String, plan: WorkoutPlan?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AccentIndigo.copy(alpha = 0.15f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = AccentIndigo, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Mesaj text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = text.replace("**", ""),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Plan card (dacă există)
            plan?.let { WorkoutPlanCard(it) }
        }
    }
}

@Composable
private fun WorkoutPlanCard(plan: WorkoutPlan) {
    val typeColor = when (plan.type.uppercase()) {
        "STRENGTH", "FORTA", "FORȚĂ" -> AccentIndigo
        "SPEED", "VITEZA", "VITEZĂ" -> AccentAmber
        "ENDURANCE", "REZISTENTA" -> AccentGreen
        "RECOVERY", "RECUPERARE" -> AccentBlue
        else -> AccentIndigo
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Warning
        plan.warning?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = 0.07f))
                    .border(1.dp, AccentRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠", fontSize = 14.sp)
                Text(it, color = AccentRed, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        // Bompa context
        plan.bompaContext?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentIndigo.copy(alpha = 0.06f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📖", fontSize = 14.sp)
                Text(it, color = AccentIndigo.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        // Plan header + steps
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSmBg)
                .border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(plan.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.12f))
                        .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(plan.type, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)

            plan.steps.forEachIndexed { index, step ->
                StepItem(step = step, typeColor = typeColor)
                if (index < plan.steps.size - 1) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.04f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }

        // Safety reminder
        plan.safetyReminder?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentAmber.copy(alpha = 0.05f))
                    .border(1.dp, AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💓", fontSize = 14.sp)
                Text(it, color = AccentAmber.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun StepItem(step: WorkoutStep, typeColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f))
                    .border(1.dp, typeColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${step.number}", color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = step.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            step.duration?.let {
                Text(it, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = step.description,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 30.dp)
        )
        if (step.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 30.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                step.tags.take(4).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(typeColor.copy(alpha = 0.1f))
                            .border(1.dp, typeColor.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(tag, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE COMUNE
// ─────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    cnsScore: Int,
    phaseName: String,
    weekNum: Int,
    totalWeeks: Int,
    todayWorkout: String,
    daysToComp: Long,
    showContext: Boolean,
    onNewSession: () -> Unit
) {
    val cnsColor = when {
        cnsScore >= 70 -> AccentGreen
        cnsScore >= 50 -> AccentAmber
        cnsScore > 0 -> AccentRed
        else -> Color.White.copy(alpha = 0.3f)
    }

    Column(modifier = Modifier.fillMaxWidth().background(BgDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(AccentIndigo.copy(alpha = 0.15f))
                    .border(1.dp, AccentIndigo.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Asistent Bompa", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Powered by Groq",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
            TextButton(
                onClick = onNewSession,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+ Sesiune nouă",
                    color = AccentIndigo.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AccentGreen))
        }

        if (showContext) {
            LazyRow(
                modifier = Modifier.fillMaxWidth()
                    .background(AccentIndigo.copy(alpha = 0.03f))
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item { ContextPill(if (cnsScore > 0) "CNS $cnsScore" else "CNS —", cnsColor) }
                item { ContextPill("${phaseName.replaceFirstChar{it.uppercase()}} · S$weekNum/$totalWeeks", AccentIndigo) }
                item { ContextPill("Azi: $todayWorkout", AccentIndigo) }
                if (daysToComp > 0) item { ContextPill("$daysToComp zile → Comp", AccentRed) }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
    }
}

@Composable
private fun ContextPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(AccentIndigo.copy(alpha = 0.15f))
                .border(1.dp, AccentIndigo.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("AI", color = AccentIndigo, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = i * 150),
                            RepeatMode.Reverse
                        ),
                        label = "dot$i"
                    )
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = alpha)))
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(BgDark)
            .padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                .background(GlassSmBg).border(1.dp, GlassSmBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(AccentIndigo),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Întreabă ceva...", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp)
                inner()
            }
        )
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (!isLoading && value.isNotBlank()) AccentIndigo.copy(alpha = 0.2f) else GlassBg)
                .border(1.dp, if (!isLoading && value.isNotBlank()) AccentIndigo.copy(alpha = 0.4f) else GlassBorder, RoundedCornerShape(12.dp))
                .clickable(enabled = !isLoading && value.isNotBlank()) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentIndigo.copy(alpha = 0.5f))
            } else {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = if (value.isNotBlank()) AccentIndigo else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(22.dp))
            }
        }
    }
}
