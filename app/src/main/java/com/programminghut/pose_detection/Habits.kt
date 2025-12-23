package com.programminghut.pose_detection


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.programminghut.pose_detection.ui.activity.NewMainActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.*

class Habits : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pose_detectionTheme {
                // Use a column layout for better appearance
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display welcome message
                    Text(
                        text = "Good Habits - Fitness Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                     )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Il tuo compagno per allenamenti intelligenti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                     )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // === ALLENAMENTI === 
                    Text(
                        text = "🏋️ ALLENAMENTI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Bottone SQUAT COUNTER (funzionalità speciale)
                    CustomButton(
                        text = "SQUAT COUNTER",
                        onClick = {
                            val intent = Intent(this@Habits, Squat::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottone ALLENAMENTI (workout strutturati)
                    CustomButton(
                        text = "ALLENAMENTI",
                        onClick = {
                            val intent = Intent(this@Habits, WorkoutTrackingMenuActivity::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottone SESSIONE GIORNALIERA (nuovo sistema modulare)
                    CustomButton(
                        text = "NUOVA APP (CLEAN ARCHITECTURE)",
                        onClick = {
                            val intent = Intent(this@Habits, NewMainActivity::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // === GESTIONE === 
                    Text(
                        text = "🔧 GESTIONE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Bottone ESERCIZI (libreria e creazione)
                    CustomButton(
                        text = "ESERCIZI",
                        onClick = {
                            val intent = Intent(this@Habits, ExerciseSelectorActivity::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottone CREA NUOVO ESERCIZIO (placeholder - funzionalità integrata in NewMainActivity)
                    CustomButton(
                        text = "CREA NUOVO ESERCIZIO [Legacy]",
                        onClick = {
                            // TODO: Funzionalità integrata nella nuova architettura (NewMainActivity > Tab Esercizi)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bottone CREA NUOVO ALLENAMENTO (placeholder - funzionalità integrata in NewMainActivity)
                    CustomButton(
                        text = "CREA NUOVO ALLENAMENTO [Legacy]",
                        onClick = {
                            // TODO: Funzionalità integrata nella nuova architettura (NewMainActivity > Tab Allenamenti)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // === CRONOLOGIA E ANALISI === 
                    Text(
                        text = "📊 CRONOLOGIA & ANALISI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bottone STORICO (metà larghezza)
                        Button(
                            onClick = {
                                val intent = Intent(this@Habits, SessionsHistoryActivity::class.java)
                                startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("STORICO")
                        }
                        
                        // Bottone DASHBOARD (metà larghezza)  
                        Button(
                            onClick = {
                                val intent = Intent(this@Habits, DashboardActivity::class.java)
                                startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DASHBOARD")
                        }
                    }
                    
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calendario temporaneamente disabilitato
                    CustomButton(
                        text = "CALENDARIO",
                        onClick = {
                            val intent = Intent(this@Habits, StreakCalendarActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Adjust padding as needed
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun CompactCalendarCard(
    onCalendarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCalendarClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Calendario",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calendario Costanza",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Tocca per espandere",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mini calendar grid (ultima settimana)
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Mostra gli ultimi 7 giorni
            val last7Days = (6 downTo 0).map { daysAgo ->
                val dayCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                }
                dayCalendar.timeInMillis
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(last7Days) { dayTimestamp ->
                    MiniCalendarDay(
                        dayTimestamp = dayTimestamp,
                        isToday = isSameDay(dayTimestamp, today),
                        hasWorkout = (dayTimestamp / (24 * 60 * 60 * 1000)) % 3 == 0L // Mock data
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "🔥 Streak attuale: 3 giorni",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MiniCalendarDay(
    dayTimestamp: Long,
    isToday: Boolean,
    hasWorkout: Boolean
) {
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val dayNumber = dayFormat.format(Date(dayTimestamp))
    
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        hasWorkout -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        hasWorkout -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true)
@Composable
fun HabitsPreview() {
    Pose_detectionTheme {
        Habits()
    }
}
