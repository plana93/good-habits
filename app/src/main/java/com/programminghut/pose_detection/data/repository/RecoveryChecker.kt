package com.programminghut.pose_detection.data.repository

import com.programminghut.pose_detection.data.model.DailySessionWithItems
import com.programminghut.pose_detection.util.recoveryDebug
import com.programminghut.pose_detection.util.todayDebug
import kotlinx.coroutines.flow.first

/**
 * Centralized logic to determine whether a given date should be considered "recovered".
 *
 * Rules:
 * - If there's a recovery workout session recorded for the date -> recovered (directRecovery)
 * - If the daily session for that date contains ONLY recovery-marked items (aiData contains 'squat_recovery' or 'recovery') -> recovered
 * - Otherwise NOT recovered
 */
class RecoveryChecker(
    private val sessionRepository: SessionRepository,
    private val dailySessionRepository: DailySessionRepository
) {

    suspend fun isDateRecoveredFinal(dateTimestamp: Long): Boolean {
        val startOfDay = getStartOfDay(dateTimestamp)

        // Check direct recovery recorded as RECOVERY session
        val directRecovery = sessionRepository.isDateRecovered(startOfDay)

        // Load any daily session with items for this date (first emission)
        val sessionWithItems: DailySessionWithItems? = dailySessionRepository.getSessionWithItemsForDate(dateTimestamp).first()

        val hasAnySession = sessionWithItems != null && sessionWithItems.items.isNotEmpty()

        var hasRecoveryItem = false
        var hasNonRecoveryItem = false
        sessionWithItems?.items?.forEach { item ->
            val ai = item.aiData
            val isRecoveryMarker = ai?.contains("squat_recovery", ignoreCase = true) == true || ai?.contains("recovery", ignoreCase = true) == true
            if (isRecoveryMarker) hasRecoveryItem = true else hasNonRecoveryItem = true
            if (hasRecoveryItem && hasNonRecoveryItem) return@forEach
        }

        val final = (hasRecoveryItem && !hasNonRecoveryItem) || (directRecovery && !hasAnySession)

        recoveryDebug("🔍 RecoveryChecker: date=${startOfDay} directRecovery=$directRecovery hasAnySession=$hasAnySession hasRecoveryItem=$hasRecoveryItem hasNonRecoveryItem=$hasNonRecoveryItem -> final=$final")

        return final
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
