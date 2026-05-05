package com.application.polarapplication.ui.theme.dashboard

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.application.polarapplication.ai.analysis.AppDatabase
import com.application.polarapplication.ai.chatbot.ChatMessage
import com.application.polarapplication.ai.chatbot.SessionSetup
import com.application.polarapplication.athletic.AthleticProfileManager
import com.application.polarapplication.model.TrainingSessionEntity
import com.application.polarapplication.polar.PolarManager
import com.application.polarapplication.services.WorkoutForegroundService
import com.application.polarapplication.ui.theme.profile.ProfileManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    private val _selectedSession = MutableStateFlow<TrainingSessionEntity?>(null)
    val selectedSession = _selectedSession.asStateFlow()

    private val _selectedWorkoutType = MutableStateFlow("STRENGTH")
    val selectedWorkoutType = _selectedWorkoutType.asStateFlow()

    @Volatile
    private var workoutSaved = false

    // Capturăm durata reală la momentul stop-ului
    private var capturedDurationSeconds = 0L

    private var timerJob: Job? = null
    private var serviceUpdateJob: Job? = null

    private var boundService: WorkoutForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? WorkoutForegroundService.WorkoutBinder
            boundService = binder?.getService()
            isServiceBound = true
            viewModelScope.launch {
                boundService?.elapsedSeconds?.collect { seconds ->
                    _elapsedSeconds.value = seconds
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isServiceBound = false
        }
    }

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _chatSetup = MutableStateFlow<SessionSetup?>(null)
    val chatSetup = _chatSetup.asStateFlow()

    private val _lastConnectedDeviceId = MutableStateFlow(
        application.getSharedPreferences("polar_prefs", Context.MODE_PRIVATE)
            .getString("last_device_id", null)
    )
    val lastConnectedDeviceId = _lastConnectedDeviceId.asStateFlow()

    private val _lastConnectedDeviceName = MutableStateFlow(
        application.getSharedPreferences("polar_prefs", Context.MODE_PRIVATE)
            .getString("last_device_name", null)
    )
    val lastConnectedDeviceName = _lastConnectedDeviceName.asStateFlow()

    private val workoutStopReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == "com.application.polarapplication.WORKOUT_STOP") {
                android.util.Log.d("WORKOUT_STOP", "Broadcast primit — workoutSaved=$workoutSaved")
                if (!workoutSaved) {
                    doSaveWorkoutSession(selectedWorkoutType.value)
                }
            }
        }
    }

    val athleteVitals = polarManager.athleteVitals
    val availableDevices = polarManager.availableDevices

    val userMaxHr: StateFlow<Int> = combine(
        profileManager.age,
        profileManager.customHrMax
    ) { age, customMax ->
        customMax ?: (208 - (0.7 * age)).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 190)

    val competitionDate: StateFlow<LocalDate?> = profileManager.competitionDateMillis
        .map { millis -> millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val athleticProfileManager = AthleticProfileManager(application)

    val planStartDate: StateFlow<LocalDate?> = profileManager.planStartDateMillis
        .map { millis -> millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions: StateFlow<List<TrainingSessionEntity>> = sessionDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        polarManager.deviceState,
        polarManager.athleteVitals,
        _isWorkoutActive
    ) { device, vitals, active ->
        DashboardUiState(device = device, vitals = vitals, isWorkoutActive = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        val filter = android.content.IntentFilter("com.application.polarapplication.WORKOUT_STOP")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                workoutStopReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                getApplication<Application>(),
                workoutStopReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
        viewModelScope.launch {
            userMaxHr.collect { maxHr -> polarManager.userMaxHr = maxHr }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(workoutStopReceiver)
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
    }

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

    fun saveLastConnectedDevice(deviceId: String, deviceName: String) {
        _lastConnectedDeviceId.value = deviceId
        _lastConnectedDeviceName.value = deviceName
        getApplication<Application>()
            .getSharedPreferences("polar_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_device_id", deviceId)
            .putString("last_device_name", deviceName)
            .apply()
    }

    fun startWorkout() {
        workoutSaved = false
        capturedDurationSeconds = 0L
        polarManager.prepareNewWorkout()
        _isWorkoutActive.value = true
        _elapsedSeconds.value = 0L

        val intent = WorkoutForegroundService.startIntent(getApplication(), selectedWorkoutType.value)
        getApplication<Application>().startForegroundService(intent)

        val bindIntent = Intent(getApplication(), WorkoutForegroundService::class.java)
        getApplication<Application>().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        serviceUpdateJob?.cancel()
        serviceUpdateJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                delay(5000L)
                if (_isWorkoutActive.value) {
                    boundService?.updateVitals(athleteVitals.value.heartRate, athleteVitals.value.calories)
                }
            }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                delay(1000L)
                if (!isServiceBound) _elapsedSeconds.value++
            }
        }
    }

    fun stopWorkout(workoutType: String) {
        android.util.Log.d("WORKOUT_STOP", "stopWorkout() chemat pentru tip: $workoutType")

        // Capturăm durata ACUM, înainte să resetăm orice
        capturedDurationSeconds = _elapsedSeconds.value
        android.util.Log.d("WORKOUT_STOP", "Durată capturată: ${capturedDurationSeconds}s")

        _selectedWorkoutType.value = workoutType
        _isWorkoutActive.value = false

        serviceUpdateJob?.cancel()
        serviceUpdateJob = null
        timerJob?.cancel()
        timerJob = null

        val stopIntent = WorkoutForegroundService.stopIntent(getApplication())
        getApplication<Application>().startService(stopIntent)

        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
            boundService = null
        }

        // Așteptăm 1 secundă ca ultimul batch PPI să fie procesat
        viewModelScope.launch {
            delay(1000L)
            doSaveWorkoutSession(workoutType)
        }
    }

    private fun doSaveWorkoutSession(workoutType: String) {
        if (workoutSaved) {
            android.util.Log.d("WORKOUT_STOP", "Sesiunea deja salvată, skip.")
            return
        }
        workoutSaved = true

        val currentVitals = athleteVitals.value
        val samplesList = polarManager.getHrSamples()
        val durationToSave = capturedDurationSeconds

        android.util.Log.d("WORKOUT_STOP", "Salvăm: ${samplesList.size} samples, durata=${durationToSave}s, TRIMP=${currentVitals.trimpScore}")

        val samplesJson = Gson().toJson(samplesList)
        val avgHr = if (samplesList.isNotEmpty()) samplesList.average().toInt() else currentVitals.heartRate
        val maxHr = if (samplesList.isNotEmpty()) samplesList.max() else currentVitals.heartRate

        viewModelScope.launch(Dispatchers.IO) {
            val newSession = TrainingSessionEntity(
                type = workoutType,
                avgHeartRate = avgHr,
                maxHeartRate = maxHr,
                finalTrimp = currentVitals.trimpScore,
                totalCalories = currentVitals.calories,
                cnsScoreAtStart = 0,
                cnsScoreAtEnd = currentVitals.cnsScore,
                hrSamples = samplesJson,
                isCompleted = true,
                durationSeconds = durationToSave // ← durata reală din timer
            )
            sessionDao.insertSession(newSession)
            android.util.Log.d("WORKOUT_STOP", "Sesiune salvată în DB: ${durationToSave}s, ${samplesList.size} samples")
            athleticProfileManager.updateAfterSession(newSession)
            athleticProfileManager.updateLoadBalance(allSessions.value)
        }
    }

    fun setWorkoutType(type: String) { _selectedWorkoutType.value = type }
    fun selectSession(session: TrainingSessionEntity?) { _selectedSession.value = session }
    fun deleteSession(session: TrainingSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) { sessionDao.deleteSession(session) }
    }

    fun checkDailyDecay() {
        viewModelScope.launch {
            athleticProfileManager.applyDailyDecay(allSessions.value)
            athleticProfileManager.updateLoadBalance(allSessions.value)
        }
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
        availableDays: Set<Int>,
        userName: String = ""
    ) {
        profileManager.saveProfile(
            newAge = age, newWeight = weight, newHeight = height,
            newGender = gender, newRhr = rhr, newCustomHrMax = customHrMax,
            newProfileImageUri = profileImageUri, newDobMillis = dobMillis,
            newAvailableDays = availableDays, newUserName = userName
        )
    }

    fun setCompetitionDate(date: LocalDate) {
        val compMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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

    fun saveChatMessages(messages: List<ChatMessage>) { _chatMessages.value = messages }
    fun saveChatSetup(setup: SessionSetup) { _chatSetup.value = setup }
    fun clearChat() { _chatMessages.value = emptyList(); _chatSetup.value = null }
}
