package com.application.polarapplication.ai.model

import com.application.polarapplication.ai.planning.MesoCycle
import java.time.LocalDate

data class TrainingPlan(
    val stratDate: LocalDate,
    val endDate: LocalDate,
    val mesoCycles: List<MesoCycle>
)