package com.programminghut.pose_detection.ui.calendar

import com.programminghut.pose_detection.data.dao.DailySessionDaySummary
import com.programminghut.pose_detection.data.model.WorkoutSession
import java.util.*

/**
 * Utility to build the day data map taking into account daily session summaries
 * This is extracted so it can be unit tested easily.
 */
internal fun buildDayDataMapWithDailySummariesImpl(
    sessions: List<WorkoutSession>,
    missedDays: List<Long>,
    dailySummaries: Map<Long, DailySessionDaySummary>
): Map<Long, DayData> {
    val dayDataMap = mutableMapOf<Long, DayData>()

    // Reuse workout-derived entries
    sessions.forEach { session ->
        val dayStart = getStartOfDayStatic(session.startTime)
        val existing = dayDataMap[dayStart]

        val dayData = when {
            session.sessionType == "RECOVERY" -> {
                DayData(
                    dayTimestamp = dayStart,
                    status = DayStatus.RECOVERED,
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                    totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                    sessions = (existing?.sessions ?: emptyList()) + session
                )
            }
            session.sessionType == "MANUAL" -> {
                DayData(
                    dayTimestamp = dayStart,
                    status = if (existing?.status == DayStatus.RECOVERED) DayStatus.RECOVERED else DayStatus.COMPLETED_MANUAL,
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                    totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                    sessions = (existing?.sessions ?: emptyList()) + session
                )
            }
            else -> {
                DayData(
                    dayTimestamp = dayStart,
                    status = if (existing?.status == DayStatus.RECOVERED) DayStatus.RECOVERED else DayStatus.COMPLETED,
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                    totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                    sessions = (existing?.sessions ?: emptyList()) + session
                )
            }
        }

        dayDataMap[dayStart] = dayData
    }

    // Apply daily summaries for days that don't already have workout completions
    // Normalize summary.date to start-of-day so keys match other sources (workout sessions, missedDays)
    dailySummaries.values.forEach { summary ->
        val dayTimestamp = getStartOfDayStatic(summary.date)
        // Only consider past or current days
        if (dayTimestamp <= getStartOfDayStatic(System.currentTimeMillis())) {
            val isToday = dayTimestamp == getStartOfDayStatic(System.currentTimeMillis())

            val status = if (isToday) {
                when {
                    summary.itemCount == 0 -> DayStatus.MISSED
                    summary.completedCount >= summary.itemCount -> DayStatus.COMPLETED
                    summary.itemCount > 0 -> DayStatus.IN_PROGRESS
                    else -> DayStatus.MISSED
                }
            } else {
                when {
                    summary.itemCount > 0 -> DayStatus.COMPLETED_DAILY
                    else -> DayStatus.MISSED
                }
            }

            val existing = dayDataMap[dayTimestamp]
            if (existing == null || existing.status == DayStatus.MISSED) {
                dayDataMap[dayTimestamp] = DayData(
                    dayTimestamp = dayTimestamp,
                    status = status,
                    sessionCount = summary.itemCount,
                    totalReps = summary.totalReps,
                    sessions = emptyList()
                )
            }
        }
    }

    // Mark missed days
    missedDays.forEach { missedDay ->
        if (!dayDataMap.containsKey(missedDay)) {
            dayDataMap[missedDay] = DayData(
                dayTimestamp = missedDay,
                status = DayStatus.MISSED,
                sessionCount = 0,
                totalReps = 0,
                sessions = emptyList()
            )
        }
    }

    return dayDataMap
}

private fun getStartOfDayStatic(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
