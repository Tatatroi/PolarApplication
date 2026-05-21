package com.application.polarapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),

    // ── Tipul principal de antrenament ────────────────────────────────────────
    // STRENGTH / ENDURANCE / SPEED / RECOVERY / REST
    val type: String,

    // ── Activitatea specifică (cum ai făcut antrenamentul) ────────────────────
    // Ex: "Running", "Cycling", "Martial Arts", "Gym", "Bodyweight", etc.
    // Gol = formula default per tip
    val activityType: String = "",

    // ── Detalii sesiune (Step 2 din UI) ──────────────────────────────────────
    val sessionGoal: String = "", // "Build Strength", "Improve Endurance", etc.
    val focusArea: String = "", // "Lower Body", "Upper Body", "Full Body", etc.
    val rpe: Int = 0, // Rate of Perceived Exertion 1-10 (completat după)

    val isCompleted: Boolean = false,

    // ── Date biometrice ───────────────────────────────────────────────────────
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val finalTrimp: Double, // TRIMP ajustat per activitate
    val totalCalories: Int,
    val cnsScoreAtStart: Int,
    val cnsScoreAtEnd: Int,
    val durationSeconds: Long = 0L,

    // ── Bompa ─────────────────────────────────────────────────────────────────
    val microCycleId: Long? = null,
    val hrSamples: String = ""
)
