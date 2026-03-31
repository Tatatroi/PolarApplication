package com.application.polarapplication.ui.theme.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.application.polarapplication.ai.analysis.AppDatabase
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.polar.PolarManager
import com.application.polarapplication.ui.theme.profile.ProfileManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val polarManager = PolarManager(application)
    private val sessionDao = AppDatabase.getDatabase(application).sessionDao()

    val profileManager = ProfileManager(application)

    // ── Workout activ ──────────────────────────────────────────────────────────
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _selectedSession = MutableStateFlow<TrainingSessionEntity?>(null)
    val selectedSession = _selectedSession.asStateFlow()

    private val _selectedWorkoutType = MutableStateFlow("STRENGTH")
    val selectedWorkoutType = _selectedWorkoutType.asStateFlow()

    // ── Polar ──────────────────────────────────────────────────────────────────
    val athleteVitals = polarManager.athleteVitals
    val availableDevices = polarManager.availableDevices

    // ── Profil ─────────────────────────────────────────────────────────────────
    val userMaxHr: StateFlow<Int> = combine(
        profileManager.age,
        profileManager.customHrMax
    ) { age, customMax ->
        customMax ?: (208 - (0.7 * age)).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 190)

    // ── Data competitiei (din ProfileManager, persistata) ─────────────────────
    val competitionDate: StateFlow<LocalDate?> = profileManager.competitionDateMillis
        .map { millis ->
            millis?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Data de start a planului (cand a fost generat) ────────────────────────
    val planStartDate: StateFlow<LocalDate?> = profileManager.planStartDateMillis
        .map { millis ->
            millis?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Sesiuni ────────────────────────────────────────────────────────────────
    val allSessions: StateFlow<List<TrainingSessionEntity>> = sessionDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── UI State combinat ──────────────────────────────────────────────────────
    val uiState: StateFlow<DashboardUiState> = combine(
        polarManager.deviceState,
        polarManager.athleteVitals,
        _isWorkoutActive
    ) { device, vitals, active ->
        DashboardUiState(device = device, vitals = vitals, isWorkoutActive = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        viewModelScope.launch {
            userMaxHr.collect { maxHr ->
                polarManager.userMaxHr = maxHr
            }
        }
    }

    // ── Actiuni ────────────────────────────────────────────────────────────────

    fun toggleConnection(deviceId: String) {
        if (uiState.value.device.isConnected) {
            polarManager.disconnectFromDevice(deviceId)
        } else {
            polarManager.connectToDevice(deviceId)
        }
    }

    fun startScanning() = polarManager.startScan()
    fun stopScanning() = polarManager.stopScan()

    fun connectToSelectedDevice(deviceId: String) = polarManager.connectToDevice(deviceId)

    fun startWorkout() {
        polarManager.prepareNewWorkout()
        _isWorkoutActive.value = true
    }

    fun stopWorkout(workoutType: String) {
        val currentVitals = uiState.value.vitals
        val samplesList = polarManager.getHrSamples()
        val samplesJson = Gson().toJson(samplesList)

        _isWorkoutActive.value = false

        val avgHr = if (samplesList.isNotEmpty()) samplesList.average().toInt() else currentVitals.heartRate
        val maxHr = if (samplesList.isNotEmpty()) samplesList.max() else currentVitals.heartRate

        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.insertSession(
                TrainingSessionEntity(
                    type = workoutType,
                    avgHeartRate = avgHr,
                    maxHeartRate = maxHr,
                    finalTrimp = currentVitals.trimpScore,
                    totalCalories = currentVitals.calories,
                    cnsScoreAtStart = 0,
                    cnsScoreAtEnd = currentVitals.cnsScore,
                    hrSamples = samplesJson,
                    isCompleted = true
                )
            )
        }
    }

    fun setWorkoutType(type: String) {
        _selectedWorkoutType.value = type
    }

    fun selectSession(session: TrainingSessionEntity?) {
        _selectedSession.value = session
    }

    fun deleteSession(session: TrainingSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) { sessionDao.deleteSession(session) }
    }

    fun saveUserProfile(
        age: Int,
        weight: Float,
        height: Int,
        gender: String,
        rhr: Int,
        customHrMax: Int?,
        profileImageUri: String?,

        dobMillis: Long?,
        availableDays: Set<Int>
    ) {
        profileManager.saveProfile(
            newAge = age,
            newWeight = weight,
            newHeight = height,
            newGender = gender,
            newRhr = rhr,
            newCustomHrMax = customHrMax,
            newProfileImageUri = profileImageUri,
            newDobMillis = dobMillis,
            newAvailableDays = availableDays
        )
    }

    fun setCompetitionDate(date: LocalDate) {
        val compMillis = date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val startMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        profileManager.saveProfile(
            newAge = profileManager.age.value,
            newWeight = profileManager.weight.value,
            newHeight = profileManager.height.value,
            newGender = profileManager.gender.value,
            newRhr = profileManager.rhr.value,
            newCustomHrMax = profileManager.customHrMax.value,
            newProfileImageUri = profileManager.profileImageUri.value,
            newCompetitionDateMillis = compMillis,
            newPlanStartDateMillis = startMillis
        )
    }
}
