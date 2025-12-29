package com.application.polarapplication.ui.theme.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.application.polarapplication.ui.theme.Indigo


/**
 * GreetingHeader(name)
 * Antetul principal de întâmpinare.
 */
@Composable
fun GreetingHeader(userName: String) {
    val firstName = userName.split(" ")[0]
    Text(
        text = "Salut, $firstName!",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}


/**
 * 🔹 PhaseCard(title, value, icon, color)
 * Cardurile mici pentru faza curentă și obiectivul zilnic.
 */
@Composable
fun PhaseCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val isDark = color == Indigo

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Indigo,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.DarkGray
                )
            }
        }
    }
}


/**
 * 🔹 WorkoutDetailsCard(workout)
 * Cardul mare care afișează detaliile antrenamentului planificat.
 */
//@Composable
//fun WorkoutDetailsCard(workout: Workout) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
//    ) {
//        Column(modifier = Modifier.padding(20.dp)) {
//            Text(
//                text = "Antrenamentul de Azi:",
//                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
//                modifier = Modifier.padding(bottom = 12.dp)
//            )
//            Text(
//                text = workout.title,
//                color = Indigo,
//                fontWeight = FontWeight.SemiBold,
//                fontSize = 20.sp,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//
//            // Detalii Durată & Focus
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(Icons.Filled.List, contentDescription = "Durată", tint = Color.Gray)
//                Spacer(modifier = Modifier.width(12.dp))
//                Text(text = "Durată Estimată:", fontWeight = FontWeight.Medium)
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(text = workout.duration)
//            }
//            Spacer(modifier = Modifier.height(12.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.Top
//            ) {
//                Icon(Icons.Filled.Favorite, contentDescription = "Focus", tint = Color.Gray)
//                Spacer(modifier = Modifier.width(12.dp))
//                Text(text = "Focus Monitorizat (Senzor):", fontWeight = FontWeight.Medium)
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(text = workout.focus, modifier = Modifier.weight(1f))
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//            Divider(color = Color.LightGray)
//        }
//    }
//}

