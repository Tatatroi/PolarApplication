package com.application.polarapplication.ui.planning

import java.util.Calendar

object PeriodizationEngine {

    fun generatePlan(targetDateMillis: Long): List<TrainingCycle> {
        val now = Calendar.getInstance().timeInMillis
        val diffMillis = targetDateMillis - now
        val weeksRemaining = (diffMillis / (1000 * 60 * 60 * 24 * 7)).toInt()

        // În Bompa, un ciclu ideal de pregătire are între 12 și 24 de săptămâni.
        // Împărțim săptămânile în: Preparatory (60%), Competitive (30%), Transition (10%)

        val prepWeeks = (weeksRemaining * 0.6).toInt()
        val compWeeks = (weeksRemaining * 0.3).toInt()
        val transWeeks = weeksRemaining - prepWeeks - compWeeks

        val plan = mutableListOf<TrainingCycle>()

        // Generăm fazele
        plan.add(TrainingCycle("Pregătire Generală", prepWeeks, "Focus pe volum și bază aerobă"))
        plan.add(TrainingCycle("Specific / Competiție", compWeeks, "Focus pe intensitate și viteză"))
        plan.add(TrainingCycle("Tapering / Tranziție", transWeeks, "Recuperare și atingerea vârfului de formă"))

        return plan
    }
}

data class TrainingCycle(val name: String, val durationWeeks: Int, val focus: String)