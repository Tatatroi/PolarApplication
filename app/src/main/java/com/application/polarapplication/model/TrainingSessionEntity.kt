package com.application.polarapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val type: String, // Ex: "Forță", "Viteză"
    val isCompleted: Boolean = false, // Dacă e FALSE, e un "Auto-save" (în caz de crash)

    // Datele tale din AthleteVitals
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val finalTrimp: Double,
    val totalCalories: Int,
    val cnsScoreAtStart: Int,
    val cnsScoreAtEnd: Int,

    // Pentru logica Bompa de mai târziu
    val microCycleId: Long? = null,
    val hrSamples: String = ""
)