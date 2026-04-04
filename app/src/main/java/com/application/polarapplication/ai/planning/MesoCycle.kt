package com.application.polarapplication.ai.planning

import java.time.LocalDate

data class MesoCycle(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val phase: String,
    val microCycle: List<MicroCycle>
)
