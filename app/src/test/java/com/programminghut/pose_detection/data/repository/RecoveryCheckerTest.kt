package com.programminghut.pose_detection.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import com.programminghut.pose_detection.data.dao.SessionDao
import com.programminghut.pose_detection.data.model.*

class RecoveryCheckerTest {
    init {
        // Disable runtime debug logging to avoid android.util.Log calls in unit tests
        com.programminghut.pose_detection.util.Logging.TODAY_DEBUG = false
        com.programminghut.pose_detection.util.Logging.CALENDAR_DEBUG = false
        com.programminghut.pose_detection.util.Logging.RECOVERY_DEBUG = false
    }

    private class FakeSessionDao(private val recoveredDates: Set<Long>) : SessionDao {
        override suspend fun insertSession(session: WorkoutSession): Long = 0L
        override suspend fun insertSessions(sessions: List<WorkoutSession>): List<Long> = emptyList()
        override fun getAllSessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override suspend fun getSessionById(sessionId: Long): WorkoutSession? = null
        override fun getSessionByIdFlow(sessionId: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionsByExerciseType(exerciseType: String): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getSessionsByTag(tag: String): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getTotalSessionsCount(): Flow<Int> = flowOf(0)
        override fun getSessionsCountByType(exerciseType: String): Flow<Int> = flowOf(0)
        override fun getTotalRepsAllTime(): Flow<Int?> = flowOf(0)
        override fun getTotalRepsByType(exerciseType: String): Flow<Int?> = flowOf(0)
        override fun getTotalWorkoutTimeSeconds(): Flow<Int?> = flowOf(0)
        override fun getAverageFormScore(): Flow<Float?> = flowOf(0f)
        override fun getRecentSessions(limit: Int): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getTodaySessions(dayStartTimestamp: Long, dayEndTimestamp: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getWeekSessions(weekStartTimestamp: Long, weekEndTimestamp: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override suspend fun getBestSessionByFormScore(exerciseType: String): WorkoutSession? = null
        override suspend fun getSessionWithMaxReps(): WorkoutSession? = null
        override suspend fun updateSession(session: WorkoutSession) {}
        override suspend fun updateSessionNotes(sessionId: Long, notes: String) {}
        override suspend fun updateSessionTags(sessionId: Long, tags: String) {}
        override suspend fun updateSyncStatus(sessionId: Long, isSynced: Boolean) {}
        override suspend fun updateExportTimestamp(sessionId: Long, exportedAt: Long) {}
        override suspend fun deleteSession(session: WorkoutSession) {}
        override suspend fun deleteSessionById(sessionId: Long) {}
        override suspend fun deleteAllSessions() {}
        override suspend fun deleteSessionsOlderThan(timestamp: Long) {}
        override fun getSessionsByType(sessionType: String): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getStreakAffectingSessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override suspend fun getSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long): List<WorkoutSession> = emptyList()
        override suspend fun hasSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long): Boolean = false
        override fun getRecoverySessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getManualSessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override suspend fun isDateAlreadyRecovered(recoveredDate: Long): Boolean = recoveredDates.contains(recoveredDate)
        override suspend fun getSessionsForCalendar(startTime: Long, endTime: Long): List<WorkoutSession> = emptyList()
        override fun getCountBySessionType(sessionType: String): Flow<Int> = flowOf(0)
    }

    private class DummyRepDao : com.programminghut.pose_detection.data.dao.RepDao {
        override suspend fun insertRep(rep: RepData): Long = 0L
        override suspend fun insertReps(reps: List<RepData>): List<Long> = emptyList()
        override fun getRepsForSession(sessionId: Long): Flow<List<RepData>> = flowOf(emptyList())
        override suspend fun getRepsForSessionOnce(sessionId: Long): List<RepData> = emptyList()
        override suspend fun getRepById(repId: Long): RepData? = null
        override fun getFlaggedRepsForSession(sessionId: Long): Flow<List<RepData>> = flowOf(emptyList())
        override suspend fun getLowDepthReps(sessionId: Long, minDepthScore: Float): List<RepData> = emptyList()
        override suspend fun getLowFormReps(sessionId: Long, minFormScore: Float): List<RepData> = emptyList()
        override suspend fun getRepCountForSession(sessionId: Long): Int = 0
        override suspend fun getAverageDepthScore(sessionId: Long): Float? = 0f
        override suspend fun getAverageFormScore(sessionId: Long): Float? = 0f
        override suspend fun getAverageSpeed(sessionId: Long): Float? = 0f
        override suspend fun getBestRepForSession(sessionId: Long): RepData? = null
        override suspend fun getWorstRepForSession(sessionId: Long): RepData? = null
        override suspend fun getFastestRepForSession(sessionId: Long): RepData? = null
        override suspend fun getSlowestRepForSession(sessionId: Long): RepData? = null
        override suspend fun getFlaggedRepCount(sessionId: Long): Int = 0
        override suspend fun updateRep(rep: RepData) {}
        override suspend fun updateReps(reps: List<RepData>) {}
        override suspend fun flagRepForReview(repId: Long, isFlagged: Boolean) {}
        override suspend fun flagMultipleRepsForReview(repIds: List<Long>) {}
        override suspend fun deleteRep(rep: RepData) {}
        override suspend fun deleteRepById(repId: Long) {}
        override suspend fun deleteRepsForSession(sessionId: Long) {}
        override suspend fun deleteAllReps() {}
    }

    private class FakeDailySessionDao : com.programminghut.pose_detection.data.dao.DailySessionDao {
        override suspend fun insertDailySession(session: DailySession): Long = 1L
        override suspend fun insertSessionItems(items: List<DailySessionItem>) {}
        override suspend fun insertSessionItem(item: DailySessionItem): Long = 1L
        override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long): DailySession? = null
        override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long): Flow<DailySession?> = flowOf(null)
        override fun getAllSessions(): Flow<List<DailySession>> = flowOf(emptyList())
    override suspend fun getSessionById(sessionId: Long): DailySession? = null
        override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long): List<Long> = emptyList()
        override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long): kotlinx.coroutines.flow.Flow<List<com.programminghut.pose_detection.data.dao.DailySessionDaySummary>> = flowOf(emptyList())
        override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long): Int = 0
        override suspend fun getSessionItems(sessionId: Long): List<DailySessionItem> = emptyList()
        override fun getSessionItemsFlow(sessionId: Long): Flow<List<DailySessionItem>> = flowOf(emptyList())
        override suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean, actualReps: Int?, actualTime: Int?, completedAt: Long?, notes: String) {}
        override suspend fun getSessionItemById(itemId: Long): DailySessionItem? = null
        override suspend fun updateSessionItem(item: DailySessionItem): Int = 0
        override suspend fun updateSession(session: DailySession) {}
        override suspend fun deleteSession(session: DailySession) {}
        override suspend fun deleteSessionItems(sessionId: Long) {}
        override suspend fun deleteSessionItem(itemId: Long) {}
        override suspend fun getCompletedItemsCount(sessionId: Long): Int = 0
        override suspend fun getTotalItemsCount(sessionId: Long): Int = 0
        override fun getTotalCountForExercise(exerciseId: Long): Flow<Int> = flowOf(0)
        override suspend fun invalidateCountCacheForExercise(exerciseId: Long): Int = 0
        override suspend fun getItemsByParentWorkout(workoutItemId: Long): List<DailySessionItem> = emptyList()
        override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {}
    }

    private class FakeDailySessionRelationDao(private val emitted: DailySessionWithItems?) : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
        override suspend fun getSessionWithItems(sessionId: Long): DailySessionWithItems? = null
        override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long): Flow<DailySessionWithItems?> = flowOf(emitted)
        override suspend fun getSessionItemsWithDetails(sessionId: Long): List<DailySessionItemWithDetails> = emptyList()
        override fun getSessionItemsWithDetailsFlow(sessionId: Long): Flow<List<DailySessionItemWithDetails>> = flowOf(emptyList())
        override suspend fun getSessionsHistory(limit: Int, offset: Int): List<com.programminghut.pose_detection.data.dao.DailySessionSummary> = emptyList()
        override fun getSessionsHistoryFlow(): Flow<List<com.programminghut.pose_detection.data.dao.DailySessionSummary>> = flowOf(emptyList())
        override suspend fun getExerciseStats(startDate: Long, endDate: Long): List<com.programminghut.pose_detection.data.dao.ExerciseStats> = emptyList()
        override suspend fun getWorkoutStats(startDate: Long, endDate: Long): List<com.programminghut.pose_detection.data.dao.WorkoutStats> = emptyList()
    }

    private class FakeExerciseDao : com.programminghut.pose_detection.data.dao.ExerciseDao {
        override suspend fun getExerciseById(exerciseId: Long): Exercise? = null
        override suspend fun getExerciseByName(name: String): Exercise? = null
        override suspend fun insertExercise(exercise: Exercise): Long = 1L
        override suspend fun updateExercise(exercise: Exercise) {}
        override fun getAllExercises(): Flow<List<Exercise>> = flowOf(emptyList())
        override suspend fun deleteAllCustomExercises() {}
        override suspend fun deleteExercise(exercise: Exercise) {}
        override suspend fun deleteExerciseById(exerciseId: Long) {}
        override suspend fun exerciseExistsByName(name: String): Boolean = false
        override suspend fun insertExercises(exercises: List<Exercise>) {}
        override fun getExerciseByIdFlow(exerciseId: Long): Flow<Exercise?> = flowOf(null)
        override fun getExercisesByType(type: com.programminghut.pose_detection.data.model.ExerciseType): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getPredefinedExercises(): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getCustomExercises(): Flow<List<Exercise>> = flowOf(emptyList())
        override fun searchExercisesByName(query: String): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getExercisesByTag(tag: String): Flow<List<Exercise>> = flowOf(emptyList())
        override suspend fun getExerciseCount(): Int = 0
        override suspend fun getCustomExerciseCount(): Int = 0
        override fun getRecentlyModifiedExercises(limit: Int): Flow<List<Exercise>> = flowOf(emptyList())
    }

    private class FakeWorkoutDao : com.programminghut.pose_detection.data.dao.WorkoutDao {
        override suspend fun insertWorkout(workout: Workout): Long = 1L
        override suspend fun insertWorkoutExercises(exercises: List<com.programminghut.pose_detection.data.model.WorkoutExercise>) {}
        override suspend fun getWorkoutById(workoutId: Long): Workout? = null
        override suspend fun getWorkoutExercises(workoutId: Long): List<com.programminghut.pose_detection.data.model.WorkoutExercise> = emptyList()
        override fun getAllWorkouts(): Flow<List<Workout>> = flowOf(emptyList())
        override fun getAllWorkoutsWithExercises(): Flow<List<com.programminghut.pose_detection.data.model.WorkoutWithExercises>> = flowOf(emptyList())
        override suspend fun getWorkoutWithExercises(workoutId: Long): com.programminghut.pose_detection.data.model.WorkoutWithExercises? = null
        override fun getWorkoutWithExercisesFlow(workoutId: Long): Flow<com.programminghut.pose_detection.data.model.WorkoutWithExercises?> = flowOf(null)
        override fun getWorkoutExercisesFlow(workoutId: Long): Flow<List<com.programminghut.pose_detection.data.model.WorkoutExercise>> = flowOf(emptyList())
        override suspend fun deleteWorkout(workout: Workout) {}
        override suspend fun deleteWorkoutById(workoutId: Long) {}
        override suspend fun deleteWorkoutExercises(workoutId: Long) {}
        override suspend fun updateWorkout(workout: Workout) {}
    }

    @Test
    fun `direct recovery with no daily session returns recovered`() = runBlocking {
        val targetDay = getStartOfDay(System.currentTimeMillis())
        val sessionRepo = SessionRepository(FakeSessionDao(setOf(targetDay)), DummyRepDao())
        val dailyRepo = DailySessionRepository(FakeDailySessionDao(), FakeDailySessionRelationDao(null), FakeExerciseDao(), FakeWorkoutDao())

        val checker = RecoveryChecker(sessionRepo, dailyRepo)

        val result = checker.isDateRecoveredFinal(targetDay)
        assertTrue(result)
    }

    @Test
    fun `daily session with only recovery items returns recovered`() = runBlocking {
        val targetDay = getStartOfDay(System.currentTimeMillis())
        val sessionRepo = SessionRepository(FakeSessionDao(emptySet()), DummyRepDao())

        val session = DailySession(sessionId = 1, date = targetDay)
        val items = listOf(
            DailySessionItem(itemId = 1, sessionId = 1, order = 0, itemType = SessionItemType.EXERCISE, exerciseId = 1, aiData = "squat_recovery"),
            DailySessionItem(itemId = 2, sessionId = 1, order = 1, itemType = SessionItemType.EXERCISE, exerciseId = 2, aiData = "recovery")
        )
        val dailyRepo = DailySessionRepository(FakeDailySessionDao(), FakeDailySessionRelationDao(DailySessionWithItems(session, items)), FakeExerciseDao(), FakeWorkoutDao())

        val checker = RecoveryChecker(sessionRepo, dailyRepo)
        val result = checker.isDateRecoveredFinal(targetDay)
        assertTrue(result)
    }

    @Test
    fun `daily session with mixed items is not recovered`() = runBlocking {
        val targetDay = getStartOfDay(System.currentTimeMillis())
        val sessionRepo = SessionRepository(FakeSessionDao(emptySet()), DummyRepDao())

        val session = DailySession(sessionId = 1, date = targetDay)
        val items = listOf(
            DailySessionItem(itemId = 1, sessionId = 1, order = 0, itemType = SessionItemType.EXERCISE, exerciseId = 1, aiData = "squat_recovery"),
            DailySessionItem(itemId = 2, sessionId = 1, order = 1, itemType = SessionItemType.EXERCISE, exerciseId = 2, aiData = null)
        )
        val dailyRepo = DailySessionRepository(FakeDailySessionDao(), FakeDailySessionRelationDao(DailySessionWithItems(session, items)), FakeExerciseDao(), FakeWorkoutDao())

        val checker = RecoveryChecker(sessionRepo, dailyRepo)
        val result = checker.isDateRecoveredFinal(targetDay)
        assertFalse(result)
    }

    @Test
    fun `no recovery and no session not recovered`() = runBlocking {
        val targetDay = getStartOfDay(System.currentTimeMillis())
        val sessionRepo = SessionRepository(FakeSessionDao(emptySet()), DummyRepDao())
        val dailyRepo = DailySessionRepository(FakeDailySessionDao(), FakeDailySessionRelationDao(null), FakeExerciseDao(), FakeWorkoutDao())

        val checker = RecoveryChecker(sessionRepo, dailyRepo)
        val result = checker.isDateRecoveredFinal(targetDay)
        assertFalse(result)
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
