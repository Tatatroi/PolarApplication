package com.application.polarapplication.ui.theme.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.application.polarapplication.ai.analysis.AppDatabase
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.polar.PolarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.gson.Gson
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val polarManager = PolarManager(application)

    // Accesăm "secretara" (DAO) pentru a salva datele
    private val sessionDao = AppDatabase.getDatabase(application).sessionDao()

    // Ținem minte dacă utilizatorul a apăsat START
    private val _isWorkoutActive = MutableStateFlow(false)
    private val _selectedSession = MutableStateFlow<TrainingSessionEntity?>(null)
    val availableDevices = polarManager.availableDevices
    fun startScanning() = polarManager.startScan()
    fun stopScanning() = polarManager.stopScan()

    val selectedSession = _selectedSession.asStateFlow()

    val allSessions: StateFlow<List<TrainingSessionEntity>> = sessionDao.getAllSessionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<DashboardUiState> = combine(
        polarManager.deviceState,
        polarManager.athleteVitals,
        _isWorkoutActive
    ) { device, vitals, active ->
        DashboardUiState(
            device = device,
            vitals = vitals,
            isWorkoutActive = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun toggleConnection(deviceId: String) {
        if (uiState.value.device.isConnected) {
            polarManager.disconnectFromDevice(deviceId)
        } else {
            polarManager.connectToDevice(deviceId)
        }
    }

    // Funcția care pornește antrenamentul
    fun startWorkout() {
        polarManager.prepareNewWorkout()
        _isWorkoutActive.value = true
        // Aici am putea reseta TRIMP-ul din PolarManager dacă vrem o sesiune nouă curată
    }

    // Funcția care oprește și SALVEAZĂ în baza de date
    fun stopWorkout(workoutType: String) {
        val currentVitals = uiState.value.vitals
        val samplesList = polarManager.getHrSamples()
        val samplesJson = Gson().toJson(samplesList) // Transformăm [120, 125, 130] în "[120,125,130]"
        _isWorkoutActive.value = false

        // Lansăm o corutină (un proces pe fundal) pentru a scrie în baza de date
        viewModelScope.launch(Dispatchers.IO) {
            val newSession = TrainingSessionEntity(
                type = workoutType,
                avgHeartRate = currentVitals.heartRate, // În viitor putem calcula media reală
                maxHeartRate = currentVitals.heartRate,
                finalTrimp = currentVitals.trimpScore,
                totalCalories = currentVitals.calories,
                cnsScoreAtStart = 0, // Vom implementa logica de captură la start
                cnsScoreAtEnd = currentVitals.cnsScore ,
                hrSamples = samplesJson,
                isCompleted = true
            )
            sessionDao.insertSession(newSession)
        }
    }

    fun selectSession(session: TrainingSessionEntity?) {
        _selectedSession.value = session
    }

    fun connectToSelectedDevice(deviceId: String) {
        polarManager.connectToDevice(deviceId)
        // Aici putem salva ID-ul în "Istoric" (SharedPreferences)
    }
}