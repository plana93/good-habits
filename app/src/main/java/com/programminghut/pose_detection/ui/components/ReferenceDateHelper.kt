package com.programminghut.pose_detection.ui.components

import java.util.Calendar

/**
 * Helper class for selecting reference dates in wellness tracker entry
 * 
 * Provides quick options like "Today", "Yesterday", "2 days ago", etc.
 * Makes it easy for users to track emotions/events retroactively.
 */
object ReferenceDateHelper {
    
    /**
     * Predefined quick date options
     */
    data class DateOption(
        val label: String,
        val daysAgo: Int,
        val timestamp: Long
    )
    
    /**
     * Get a list of quick date options (Today, Yesterday, 2 days ago, etc.)
     * 
     * @param maxDaysAgo Maximum number of days in the past to show (default: 7)
     * @return List of DateOption for quick selection
     */
    fun getQuickDateOptions(maxDaysAgo: Int = 7): List<DateOption> {
        val options = mutableListOf<DateOption>()
        val calendar = Calendar.getInstance()
        
        for (daysAgo in 0..maxDaysAgo) {
            // Calculate timestamp for this date
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 12) // Set to noon to avoid timezone issues
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            
            val label = when (daysAgo) {
                0 -> "Today"
                1 -> "Yesterday"
                2 -> "2 days ago"
                3 -> "3 days ago"
                else -> "$daysAgo days ago"
            }
            
            options.add(
                DateOption(
                    label = label,
                    daysAgo = daysAgo,
                    timestamp = calendar.timeInMillis
                )
            )
        }
        
        return options
    }
    
    /**
     * Get timestamp for a specific number of days ago
     * 
     * @param daysAgo Number of days in the past (0 = today)
     * @return Timestamp for that date at noon
     */
    fun getTimestampForDaysAgo(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return calendar.timeInMillis
    }
    
    /**
     * Format a date option for display
     * Shows "Today" for today, "Yesterday" for yesterday, otherwise shows the actual date
     * 
     * @param timestamp The timestamp to format
     * @return Formatted string like "Today", "Yesterday", or "Jan 2, 2026"
     */
    fun formatDateOption(timestamp: Long): String {
        val daysAgo = calculateDaysAgo(timestamp)
        return when (daysAgo) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.ENGLISH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val year = calendar.get(Calendar.YEAR)
                "$month $day, $year"
            }
        }
    }
    
    /**
     * Calculate how many days ago a timestamp is from today
     * 
     * @param timestamp The timestamp to check
     * @return Number of days ago (0 = today, 1 = yesterday, etc.)
     */
    fun calculateDaysAgo(timestamp: Long): Int {
        val msPerDay = 24 * 60 * 60 * 1000
        val todayStart = (System.currentTimeMillis() / msPerDay) * msPerDay
        val timestampDayStart = (timestamp / msPerDay) * msPerDay
        return ((todayStart - timestampDayStart) / msPerDay).toInt()
    }
    
    /**
     * Check if a timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        return calculateDaysAgo(timestamp) == 0
    }
    
    /**
     * Check if a timestamp is yesterday
     */
    fun isYesterday(timestamp: Long): Boolean {
        return calculateDaysAgo(timestamp) == 1
    }
    
    /**
     * Get a descriptive label for days ago
     * Used in the UI to show context like "Added today about yesterday"
     * 
     * @param daysAgo Number of days ago
     * @return Human-readable description
     */
    fun getDaysAgoLabel(daysAgo: Int): String {
        return when (daysAgo) {
            0 -> "today"
            1 -> "yesterday"
            else -> "$daysAgo days ago"
        }
    }
}

/**
 * Example UI composable structure (to be implemented)
 * 
 * This shows how the date picker would work in the wellness tracker entry screen:
 * 
 * @Composable
 * fun WellnessTrackerEntryDialog(
 *     tracker: WellnessTrackerTemplate,
 *     onSave: (TrackerResponse) -> Unit,
 *     onDismiss: () -> Unit
 * ) {
 *     var selectedReferenceDate by remember { mutableStateOf(System.currentTimeMillis()) }
 *     var showDatePicker by remember { mutableStateOf(false) }
 *     
 *     Dialog(onDismissRequest = onDismiss) {
 *         Card {
 *             Column {
 *                 // Tracker name and description
 *                 Text(tracker.name)
 *                 
 *                 // Reference date selector
 *                 Text("When did this happen?")
 *                 
 *                 // Quick date buttons
 *                 LazyRow {
 *                     items(ReferenceDateHelper.getQuickDateOptions(7)) { option ->
 *                         FilterChip(
 *                             selected = option.timestamp == selectedReferenceDate,
 *                             onClick = { selectedReferenceDate = option.timestamp },
 *                             label = { Text(option.label) }
 *                         )
 *                     }
 *                 }
 *                 
 *                 // Or open date picker for custom date
 *                 TextButton(onClick = { showDatePicker = true }) {
 *                     Text("Pick different date...")
 *                 }
 *                 
 *                 // Rating/emotion/text input
 *                 // ...
 *                 
 *                 // Save button
 *                 Button(onClick = {
 *                     val response = TrackerResponse(
 *                         trackerId = tracker.id,
 *                         trackerName = tracker.name,
 *                         responseType = tracker.responseType,
 *                         ratingValue = selectedRating,
 *                         timestamp = System.currentTimeMillis(), // NOW
 *                         referenceDate = selectedReferenceDate   // Could be past
 *                     )
 *                     onSave(response)
 *                 }) {
 *                     Text("Save")
 *                 }
 *             }
 *         }
 *     }
 * }
 */
