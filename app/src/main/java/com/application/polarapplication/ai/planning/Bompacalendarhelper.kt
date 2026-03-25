package com.application.polarapplication.ai.planning

import androidx.compose.ui.graphics.Color
import com.application.polarapplication.ai.daily.WorkoutType
import java.time.LocalDate

object BompaCalendarHelper {

    fun getPlannedWorkout(
        date: LocalDate,
        planStartDate: LocalDate,
        competitionDate: LocalDate
    ): WorkoutType? {
        if (date.isBefore(planStartDate) || !date.isBefore(competitionDate)) return null

        val planner = TrainingPlanner()
        val plan = planner.generatePlan(competitionDate, planStartDate)

        val micro = plan.mesoCycles
            .flatMap { it.microCycle }
            .firstOrNull { !it.startDate.isAfter(date) && !it.endDate.isBefore(date) }
            ?: return null

        val dayIndex = java.time.temporal.ChronoUnit.DAYS
            .between(micro.startDate, date).toInt()

        return micro.workouts.getOrNull(dayIndex)
    }

    fun getPhaseBackground(
        date: LocalDate,
        planStartDate: LocalDate,
        competitionDate: LocalDate
    ): Color? {
        if (date.isBefore(planStartDate) || !date.isBefore(competitionDate)) return null

        val planner = TrainingPlanner()
        val plan = planner.generatePlan(competitionDate, planStartDate)
        val meso = plan.mesoCycles.firstOrNull {
            !it.startDate.isAfter(date) && it.endDate.isAfter(date)
        } ?: return null

        return when (meso.phase.lowercase()) {
            "general"  -> Color(0xFF1D3A2A)
            "specific" -> Color(0xFF2A1F00)
            "precomp"  -> Color(0xFF1A0D2E)
            "comp"     -> Color(0xFF1A0808)
            "recovery" -> Color(0xFF0D1A1A)
            else       -> null
        }
    }

    fun workoutLetter(type: WorkoutType): String = when (type) {
        WorkoutType.STRENGTH  -> "F"
        WorkoutType.ENDURANCE -> "R"
        WorkoutType.SPEED     -> "V"
        WorkoutType.RECOVERY  -> "Rc"
        WorkoutType.REST      -> "—"
    }

    fun workoutColor(type: WorkoutType): Color = when (type) {
        WorkoutType.STRENGTH  -> Color(0xFF818CF8)
        WorkoutType.ENDURANCE -> Color(0xFF4ADE80)
        WorkoutType.SPEED     -> Color(0xFFFBBF24)
        WorkoutType.RECOVERY  -> Color(0xFF60A5FA)
        WorkoutType.REST      -> Color(0xFF444455)
    }

    fun isOffPlan(actualType: String, plannedType: WorkoutType?): Boolean {
        if (plannedType == null || plannedType == WorkoutType.REST) return false
        return actualType.uppercase() != plannedType.name.uppercase()
    }
}