package com.application.polarapplication.ai.planning

import com.application.polarapplication.ai.daily.WorkoutType

object MicroCycleGenerator {

    fun generate(
        phase: String,
        availableDays: Set<Int> = setOf(1, 2, 3, 4, 5),
        focus: String = "full" // "full", "strength", "endurance", "speed"
    ): List<WorkoutType> {

        // Sesiunile de bază per fază
        val baseSessions = when (phase) {
            "general" -> listOf(WorkoutType.STRENGTH, WorkoutType.ENDURANCE, WorkoutType.STRENGTH, WorkoutType.ENDURANCE, WorkoutType.SPEED)
            "specific" -> listOf(WorkoutType.STRENGTH, WorkoutType.SPEED, WorkoutType.ENDURANCE, WorkoutType.STRENGTH, WorkoutType.SPEED)
            "precomp" -> listOf(WorkoutType.STRENGTH, WorkoutType.ENDURANCE, WorkoutType.SPEED, WorkoutType.STRENGTH, WorkoutType.SPEED)
            "comp" -> listOf(WorkoutType.SPEED, WorkoutType.SPEED, WorkoutType.RECOVERY, WorkoutType.SPEED, WorkoutType.RECOVERY)
            "recovery" -> listOf(WorkoutType.RECOVERY, WorkoutType.RECOVERY, WorkoutType.RECOVERY, WorkoutType.RECOVERY, WorkoutType.RECOVERY)
            else -> listOf(WorkoutType.REST, WorkoutType.REST, WorkoutType.REST, WorkoutType.REST, WorkoutType.REST)
        }

        // Ajustează sesiunile în funcție de focus
        val focusedSessions = applyFocus(baseSessions, focus, availableDays.size)

        // Distribuie sesiunile în cele 7 zile
        val week = MutableList(7) { WorkoutType.REST }
        val sortedAvailable = availableDays.map { it - 1 }.sorted() // 0-based index

        // Inserează o zi de RECOVERY dacă planul nu e recovery și avem suficiente zile
        val sessionsWithRecovery = if (phase != "recovery" && focusedSessions.size >= 3) {
            insertRecovery(focusedSessions, sortedAvailable.size)
        } else {
            focusedSessions
        }

        // Mapează sesiunile pe zilele disponibile
        sessionsWithRecovery.take(sortedAvailable.size).forEachIndexed { i, workout ->
            week[sortedAvailable[i]] = workout
        }

        return week
    }

    private fun applyFocus(base: List<WorkoutType>, focus: String, availableDaysCount: Int): List<WorkoutType> {
        val targetType = when (focus) {
            "strength" -> WorkoutType.STRENGTH
            "endurance" -> WorkoutType.ENDURANCE
            "speed" -> WorkoutType.SPEED
            else -> null // full
        }

        if (targetType == null) {
            // Full — echilibrat: câte o treime din fiecare
            return buildBalanced(availableDaysCount)
        }

        // Focusat — majoritate din tipul ales, restul complementare
        val result = mutableListOf<WorkoutType>()
        val focusCount = when {
            availableDaysCount <= 3 -> 2
            availableDaysCount == 4 -> 3
            availableDaysCount == 5 -> 3
            availableDaysCount == 6 -> 4
            else -> 4
        }
        val otherCount = availableDaysCount - focusCount

        repeat(focusCount) { result.add(targetType) }

        // Complementare — celelalte 2 tipuri alternând
        val complementary = WorkoutType.entries
            .filter { it != targetType && it != WorkoutType.REST && it != WorkoutType.RECOVERY }
        repeat(otherCount) { i -> result.add(complementary[i % complementary.size]) }

        return result.shuffled().let { shuffled ->
            // Nu lăsa același tip de 2 ori consecutiv dacă posibil
            spreadSessions(shuffled)
        }
    }

    private fun buildBalanced(count: Int): List<WorkoutType> {
        val types = listOf(WorkoutType.STRENGTH, WorkoutType.ENDURANCE, WorkoutType.SPEED)
        val result = mutableListOf<WorkoutType>()
        repeat(count) { i -> result.add(types[i % types.size]) }
        return spreadSessions(result)
    }

    private fun insertRecovery(sessions: List<WorkoutType>, availableDays: Int): List<WorkoutType> {
        if (availableDays < 4) return sessions // prea puține zile, nu inserăm recovery
        val result = sessions.toMutableList()
        // Pune RECOVERY la mijloc
        val mid = result.size / 2
        if (result.size > mid && result[mid] != WorkoutType.RECOVERY) {
            result[mid] = WorkoutType.RECOVERY
        }
        return result
    }

    // Spread — evită același tip consecutiv
    private fun spreadSessions(sessions: List<WorkoutType>): List<WorkoutType> {
        val result = sessions.toMutableList()
        for (i in 1 until result.size) {
            if (result[i] == result[i - 1]) {
                // Caută un element diferit mai departe și swap
                val swapIdx = (i + 1 until result.size).firstOrNull { result[it] != result[i] }
                if (swapIdx != null) {
                    val tmp = result[i]
                    result[i] = result[swapIdx]
                    result[swapIdx] = tmp
                }
            }
        }
        return result
    }
}