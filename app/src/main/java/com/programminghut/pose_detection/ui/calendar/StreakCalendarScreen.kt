package com.programminghut.pose_detection.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * StreakCalendarScreen - Calendario della Costanza
 * 
 * Phase 4: Session Recovery & Calendar
 * Shows monthly calendar with workout days, missed days, and recovery status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakCalendarScreen(
    viewModel: CalendarViewModel,
    onBackClick: () -> Unit,
    onDayClick: (Long, DayStatus) -> Unit = { _, _ -> },
    onRecoveryClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var currentMonthOffset by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario Costanza") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Info dialog */ }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is CalendarUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is CalendarUiState.Success -> {
                CalendarContent(
                    dayDataMap = state.dayDataMap,
                    currentStreak = state.currentStreak,
                    monthStart = state.monthStart,
                    monthOffset = currentMonthOffset,
                    selectedDate = selectedDate,
                    onMonthChange = { offset ->
                        currentMonthOffset = offset
                        viewModel.loadCalendarData(offset)
                    },
                    onDayClick = { timestamp, status ->
                        // ✅ NON chiamiamo selectDate automaticamente per evitare navigazioni indesiderate
                        // viewModel.selectDate(timestamp)  // RIMOSSA - causava navigazione automatica
                        onDayClick(timestamp, status)
                    },
                    onRecoveryClick = onRecoveryClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is CalendarUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarContent(
    dayDataMap: Map<Long, DayData>,
    currentStreak: Int,
    monthStart: Long,
    monthOffset: Int,
    selectedDate: Long?,
    onMonthChange: (Int) -> Unit,
    onDayClick: (Long, DayStatus) -> Unit,
    onRecoveryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak indicator
        StreakCard(currentStreak = currentStreak)
        
        // Month navigation
        MonthNavigator(
            monthStart = monthStart,
            monthOffset = monthOffset,
            onPreviousMonth = { onMonthChange(monthOffset - 1) },
            onNextMonth = { onMonthChange(monthOffset + 1) }
        )
        
        // Legend
        CalendarLegend()
        
        // Calendar grid
        MonthCalendarGrid(
            dayDataMap = dayDataMap,
            monthStart = monthStart,
            selectedDate = selectedDate,
            onDayClick = onDayClick
        )
        
        // Selected day details
        selectedDate?.let { timestamp ->
            dayDataMap[timestamp]?.let { dayData ->
                DayDetailCard(
                    dayData = dayData,
                    onRecoveryClick = { onRecoveryClick(timestamp) }
                )
            }
        }
    }
}

@Composable
fun StreakCard(currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentStreak > 0) 
                Color(0xFFFF9800).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFFF9800)
                )
                Column {
                    Text(
                        text = "Streak Attuale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentStreak > 0) 
                            "Mantieni il ritmo!" 
                        else 
                            "Inizia oggi!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "$currentStreak 🔥",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun MonthNavigator(
    monthStart: Long,
    monthOffset: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Mese precedente")
        }
        
        Text(
            text = formatMonthYear(monthStart),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onNextMonth,
            enabled = monthOffset < 0 // Can't go to future months
        ) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Mese successivo")
        }
    }
}

@Composable
fun CalendarLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Legenda",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF4CAF50), label = "Completato", icon = "✓")
                LegendItem(color = Color(0xFFFF5722), label = "Mancato", icon = "✗")
                LegendItem(color = Color(0xFF2196F3), label = "Recuperato", icon = "↺")
                LegendItem(color = Color(0xFF9E9E9E), label = "Futuro", icon = "○")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MonthCalendarGrid(
    dayDataMap: Map<Long, DayData>,
    monthStart: Long,
    selectedDate: Long?,
    onDayClick: (Long, DayStatus) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = monthStart
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    
    // Create list of all days to display
    val days = mutableListOf<CalendarDay>()
    
    // Add empty cells for alignment
    repeat(firstDayOfWeek) {
        days.add(CalendarDay.Empty)
    }
    
    // Add days of the month
    for (day in 1..daysInMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val dayTimestamp = calendar.timeInMillis
        val dayData = dayDataMap[dayTimestamp]
        val isFuture = dayTimestamp > System.currentTimeMillis()
        
        val status = when {
            isFuture -> DayStatus.FUTURE
            dayData != null -> dayData.status
            else -> DayStatus.MISSED
        }
        
        days.add(CalendarDay.Day(day, dayTimestamp, status, dayData))
    }
    
    Column {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("D", "L", "M", "M", "G", "V", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days) { calendarDay ->
                when (calendarDay) {
                    is CalendarDay.Empty -> {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                    is CalendarDay.Day -> {
                        DayCell(
                            day = calendarDay.dayNumber,
                            status = calendarDay.status,
                            isSelected = calendarDay.timestamp == selectedDate,
                            sessionCount = calendarDay.dayData?.sessionCount ?: 0,
                            onClick = { onDayClick(calendarDay.timestamp, calendarDay.status) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    status: DayStatus,
    isSelected: Boolean,
    sessionCount: Int,
    onClick: () -> Unit
) {
    val backgroundColor = when (status) {
        DayStatus.COMPLETED, DayStatus.COMPLETED_MANUAL -> Color(0xFF4CAF50)
        DayStatus.RECOVERED -> Color(0xFF2196F3)
        DayStatus.MISSED -> Color(0xFFFF5722)
        DayStatus.FUTURE -> Color(0xFF9E9E9E)
    }
    
    val icon = when (status) {
        DayStatus.COMPLETED -> "✓"
        DayStatus.COMPLETED_MANUAL -> "✎"
        DayStatus.RECOVERED -> "↺"
        DayStatus.MISSED -> "✗"
        DayStatus.FUTURE -> ""
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (isSelected)
                    backgroundColor.copy(alpha = 0.3f)
                else
                    backgroundColor.copy(alpha = if (status == DayStatus.FUTURE) 0.3f else 0.8f)
            )
            .clickable(enabled = status != DayStatus.FUTURE) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (icon.isNotEmpty()) {
                Text(
                    text = icon,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (sessionCount > 1) {
                Text(
                    text = "×$sessionCount",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                )
            }
        }
    }
}

@Composable
fun DayDetailCard(
    dayData: DayData,
    onRecoveryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatDate(dayData.dayTimestamp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (dayData.status == DayStatus.MISSED) {
                Text(
                    text = "Giorno mancato",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = onRecoveryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recupera Giorno (50+ reps)")
                }
            } else {
                Text(
                    text = "${dayData.sessionCount} sessione${if (dayData.sessionCount > 1) "i" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${dayData.totalReps} ripetizioni totali",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (dayData.status == DayStatus.RECOVERED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Giorno recuperato!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sealed class for calendar day representation
 */
sealed class CalendarDay {
    object Empty : CalendarDay()
    data class Day(
        val dayNumber: Int,
        val timestamp: Long,
        val status: DayStatus,
        val dayData: DayData?
    ) : CalendarDay()
}

/**
 * Helper functions
 */
private fun formatMonthYear(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
    return sdf.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ITALIAN)
    return sdf.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
}
