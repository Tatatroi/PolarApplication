package com.application.polarapplication.ui.theme.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.polarapplication.ai.analysis.Converters
import com.application.polarapplication.model.TrainingSessionEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun WorkoutDetailsScreen(session: TrainingSessionEntity) {
    // Folosim translatorul nostru (Converters) pentru a lua numerele din String-ul salvat
    val converters = Converters()
    val hrPoints = converters.fromString(session.hrSamples)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Detalii ${session.type}", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        // GRAFICUL VICO
        if (hrPoints.isNotEmpty()) {
            // Transformăm lista de Int în FloatEntry (ceea ce vrea Vico)
            val entries = hrPoints.mapIndexed { index, value ->
                com.patrykandpatrick.vico.core.entry.entryOf(index.toFloat(), value.toFloat())
            }

            // Creăm modelul corect
            val chartEntryModel = com.patrykandpatrick.vico.core.entry.entryModelOf(entries)

            Chart(
                chart = lineChart(
                    // Forțăm axa Y să se adapteze la valorile reale, nu să fie fixă
                    axisValuesOverrider = AxisValuesOverrider.fixed(
                        minY = hrPoints.minOrNull()?.toFloat()?.minus(10f),
                        maxY = hrPoints.maxOrNull()?.toFloat()?.plus(10f)
                    )
                ),
                model = chartEntryModel,
                startAxis = startAxis(
                    valueFormatter = { value, _ -> "${value.toInt()} bpm" }
                ),
                bottomAxis = bottomAxis(),
                modifier = Modifier.padding(16.dp).fillMaxWidth().height(250.dp)
            )
        }

        // Mai jos poți pune statistici mari (Max BPM, TRIMP, etc.)
    }
}