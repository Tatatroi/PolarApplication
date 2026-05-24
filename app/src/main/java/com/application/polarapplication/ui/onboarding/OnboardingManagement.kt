package com.application.polarapplication.ui.onboarding

import android.content.Context

class OnboardingManager(context: Context) {
    private val prefs = context.getSharedPreferences("athleteiq_onboarding", Context.MODE_PRIVATE)

    val isCompleted: Boolean
        get() = prefs.getBoolean("completed", false)

    fun markCompleted() {
        prefs.edit().putBoolean("completed", true).apply()
    }
}