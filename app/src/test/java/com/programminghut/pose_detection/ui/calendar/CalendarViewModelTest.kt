package com.programminghut.pose_detection.ui.calendar

import com.programminghut.pose_detection.data.dao.DailySessionDaySummary
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.DailySessionRepository
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class CalendarViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Disable runtime debug logging to avoid android.util.Log calls in unit tests
        com.programminghut.pose_detection.util.Logging.TODAY_DEBUG = false
        com.programminghut.pose_detection.util.Logging.CALENDAR_DEBUG = false
        com.programminghut.pose_detection.util.Logging.RECOVERY_DEBUG = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `daily summaries make missed day appear completed in calendar`() = runBlocking {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val targetDay = cal.timeInMillis

        val dailySummary = DailySessionDaySummary(date = targetDay, itemCount = 2, completedCount = 2, totalReps = 10)

        // Call the extracted implementation directly
    val dayDataMap = buildDayDataMapWithDailySummariesImpl(emptyList(), listOf(targetDay), mapOf(targetDay to dailySummary))

        val dayData = dayDataMap[targetDay]
        // Expect the day to be marked as COMPLETED_DAILY (because it has daily session items)
        assertEquals(DayStatus.COMPLETED_DAILY, dayData?.status)
    }

    @Test
    fun `daily summary date not normalized still maps to startOfDay`() = runBlocking {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY,12); set(Calendar.MINUTE,30); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val rawDate = cal.timeInMillis
        val normalizedCal = Calendar.getInstance().apply { timeInMillis = rawDate; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val targetDay = normalizedCal.timeInMillis

        val dailySummary = DailySessionDaySummary(date = rawDate, itemCount = 1, completedCount = 1, totalReps = 5)

        val dayDataMap = buildDayDataMapWithDailySummariesImpl(emptyList(), listOf(targetDay), mapOf(rawDate to dailySummary))

        val dayData = dayDataMap[targetDay]
        assertEquals(DayStatus.COMPLETED_DAILY, dayData?.status)
    }

    @Test
    fun `calendar viewmodel reacts to daily summaries updates`() = runBlocking {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val targetDay = cal.timeInMillis

        val dailySummary = DailySessionDaySummary(date = targetDay, itemCount = 2, completedCount = 2, totalReps = 10)

        // Mutable flow to simulate DB updates
        val summariesFlow = MutableStateFlow<List<DailySessionDaySummary>>(emptyList())

        // Fake DAOs to construct repositories
        val fakeDailyDao = object : com.programminghut.pose_detection.data.dao.DailySessionDao {
            override suspend fun insertDailySession(session: com.programminghut.pose_detection.data.model.DailySession): Long = 1L
            override suspend fun insertSessionItems(items: List<com.programminghut.pose_detection.data.model.DailySessionItem>) {}
            override suspend fun insertSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem): Long = 1L
            override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long): com.programminghut.pose_detection.data.model.DailySession? = null
            override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySession?)
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySession>())
            override suspend fun getSessionById(sessionId: Long) = null
            override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long) = emptyList<Long>()
            override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long) = summariesFlow
            override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long): Int = 0
            override suspend fun getSessionItems(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override fun getSessionItemsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>())
            override suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean, actualReps: Int?, actualTime: Int?, completedAt: Long?, notes: String) {}
            override suspend fun getSessionItemById(itemId: Long) = null
            override suspend fun updateSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem) = 0
            override suspend fun updateSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSessionItems(sessionId: Long) {}
            override suspend fun deleteSessionItem(itemId: Long) {}
            override suspend fun getCompletedItemsCount(sessionId: Long) = 0
            override suspend fun getTotalItemsCount(sessionId: Long) = 0
            override fun getTotalCountForExercise(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(0)
            override suspend fun invalidateCountCacheForExercise(exerciseId: Long) = 0
            override suspend fun getItemsByParentWorkout(workoutItemId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {}
        }

        val fakeSessionDao = object : com.programminghut.pose_detection.data.dao.SessionDao {
            override suspend fun insertSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) = 1L
            override suspend fun insertSessions(sessions: List<com.programminghut.pose_detection.data.model.WorkoutSession>) = emptyList<Long>()
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getSessionById(sessionId: Long) = null
            override fun getSessionByIdFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.WorkoutSession?)
            override fun getSessionsByExerciseType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getSessionsByDateRange(startTime: Long, endTime: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getSessionsByTag(tag: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getTotalSessionsCount() = kotlinx.coroutines.flow.flowOf(0)
            override fun getSessionsCountByType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalRepsAllTime() = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalRepsByType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalWorkoutTimeSeconds() = kotlinx.coroutines.flow.flowOf(0)
            override fun getAverageFormScore() = kotlinx.coroutines.flow.flowOf(0f)
            override fun getRecentSessions(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getTodaySessions(dayStartTimestamp: Long, dayEndTimestamp: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getWeekSessions(weekStartTimestamp: Long, weekEndTimestamp: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getBestSessionByFormScore(exerciseType: String) = null
            override suspend fun getSessionWithMaxReps() = null
            override suspend fun updateSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) {}
            override suspend fun updateSessionNotes(sessionId: Long, notes: String) {}
            override suspend fun updateSessionTags(sessionId: Long, tags: String) {}
            override suspend fun updateSyncStatus(sessionId: Long, isSynced: Boolean) {}
            override suspend fun updateExportTimestamp(sessionId: Long, exportedAt: Long) {}
            override suspend fun deleteSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) {}
            override suspend fun deleteSessionById(sessionId: Long) {}
            override suspend fun deleteAllSessions() {}
            override suspend fun deleteSessionsOlderThan(timestamp: Long) {}
            override fun getSessionsByType(sessionType: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getStreakAffectingSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>()
            override suspend fun hasSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long) = false
            override fun getRecoverySessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getManualSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun isDateAlreadyRecovered(recoveredDate: Long) = false
            override suspend fun getSessionsForCalendar(startTime: Long, endTime: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>()
            override fun getCountBySessionType(sessionType: String) = kotlinx.coroutines.flow.flowOf(0)
        }

        val fakeDailyRelationDao = object : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
            override suspend fun getSessionWithItems(sessionId: Long) = null
            override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySessionWithItems?)
            override suspend fun getSessionItemsWithDetails(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>()
            override fun getSessionItemsWithDetailsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>())
            override suspend fun getSessionsHistory(limit: Int, offset: Int) = emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>()
            override fun getSessionsHistoryFlow() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>())
            override suspend fun getExerciseStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.ExerciseStats>()
            override suspend fun getWorkoutStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.WorkoutStats>()
        }

        val fakeExerciseDao = object : com.programminghut.pose_detection.data.dao.ExerciseDao {
            override suspend fun getExerciseById(exerciseId: Long) = null
            override suspend fun getExerciseByName(name: String) = null
            override suspend fun insertExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) = 1L
            override suspend fun updateExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) {}
            override fun getAllExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override suspend fun deleteAllCustomExercises() {}
            override suspend fun deleteExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) {}
            override suspend fun deleteExerciseById(exerciseId: Long) {}
            override suspend fun exerciseExistsByName(name: String) = false
            override suspend fun insertExercises(exercises: List<com.programminghut.pose_detection.data.model.Exercise>) {}
            override fun getExerciseByIdFlow(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.Exercise?)
            override fun getExercisesByType(type: com.programminghut.pose_detection.data.model.ExerciseType) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getPredefinedExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getCustomExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun searchExercisesByName(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getExercisesByTag(tag: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override suspend fun getExerciseCount() = 0
            override suspend fun getCustomExerciseCount() = 0
            override fun getRecentlyModifiedExercises(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
        }

        val fakeWorkoutDao = object : com.programminghut.pose_detection.data.dao.WorkoutDao {
            override suspend fun insertWorkout(workout: com.programminghut.pose_detection.data.model.Workout) = 1L
            override suspend fun insertWorkoutExercises(exercises: List<com.programminghut.pose_detection.data.model.WorkoutExercise>) {}
            override suspend fun getWorkoutById(workoutId: Long) = null
            override suspend fun getWorkoutExercises(workoutId: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutExercise>()
            override fun getAllWorkouts() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Workout>())
            override fun getAllWorkoutsWithExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutWithExercises>())
            override suspend fun getWorkoutWithExercises(workoutId: Long) = null
            override fun getWorkoutWithExercisesFlow(workoutId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.WorkoutWithExercises?)
            override fun getWorkoutExercisesFlow(workoutId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutExercise>())
            override suspend fun deleteWorkout(workout: com.programminghut.pose_detection.data.model.Workout) {}
            override suspend fun deleteWorkoutById(workoutId: Long) {}
            override suspend fun deleteWorkoutExercises(workoutId: Long) {}
            override suspend fun updateWorkout(workout: com.programminghut.pose_detection.data.model.Workout) {}
        }

        val dummyRepDao = object : com.programminghut.pose_detection.data.dao.RepDao {
            override suspend fun insertRep(rep: com.programminghut.pose_detection.data.model.RepData): Long = 0L
            override suspend fun insertReps(reps: List<com.programminghut.pose_detection.data.model.RepData>): List<Long> = emptyList()
            override fun getRepsForSession(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.RepData>())
            override suspend fun getRepsForSessionOnce(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getRepById(repId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override fun getFlaggedRepsForSession(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.RepData>())
            override suspend fun getLowDepthReps(sessionId: Long, minDepthScore: Float) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getLowFormReps(sessionId: Long, minFormScore: Float) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getRepCountForSession(sessionId: Long) = 0
            override suspend fun getAverageDepthScore(sessionId: Long) = 0f
            override suspend fun getAverageFormScore(sessionId: Long) = 0f
            override suspend fun getAverageSpeed(sessionId: Long) = 0f
            override suspend fun getBestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getWorstRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getFastestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getSlowestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getFlaggedRepCount(sessionId: Long) = 0
            override suspend fun updateRep(rep: com.programminghut.pose_detection.data.model.RepData) {}
            override suspend fun updateReps(reps: List<com.programminghut.pose_detection.data.model.RepData>) {}
            override suspend fun flagRepForReview(repId: Long, isFlagged: Boolean) {}
            override suspend fun flagMultipleRepsForReview(repIds: List<Long>) {}
            override suspend fun deleteRep(rep: com.programminghut.pose_detection.data.model.RepData) {}
            override suspend fun deleteRepById(repId: Long) {}
            override suspend fun deleteRepsForSession(sessionId: Long) {}
            override suspend fun deleteAllReps() {}
        }

        val sessionRepo = SessionRepository(fakeSessionDao, dummyRepDao)
        val dailyRepo = DailySessionRepository(fakeDailyDao, fakeDailyRelationDao, fakeExerciseDao, fakeWorkoutDao)

        val vm = CalendarViewModel(sessionRepo, dailyRepo)

        // Let ViewModel start and collect the first emission
        testScheduler.advanceUntilIdle()
        // Ensure repository triggers a re-query so collectors pick up current DAO state deterministically in tests
        dailyRepo.triggerSessionUpdateForTests()
        testScheduler.advanceUntilIdle()

        // Initially should report MISSED for the day
        var initialState: CalendarUiState? = null
        repeat(10) {
            val s = vm.uiState
            val value = s.value
            if (value is CalendarUiState.Success) {
                initialState = value
                return@repeat
            }
            // advance scheduler so background coroutines can run
            testScheduler.advanceUntilIdle()
            delay(10)
        }

        val initialStatus = (initialState as? CalendarUiState.Success)?.dayDataMap?.get(targetDay)?.status
        // Either MISSED or null (not present) initially
        // Now emit the daily summary (simulating DB update)
        summariesFlow.value = listOf(dailySummary)
    // Force repository-level update trigger so merged flow emits in test environment
    dailyRepo.triggerSessionUpdateForTests()

        // Wait for ViewModel to pick up update
        testScheduler.advanceUntilIdle()

        var updatedState: CalendarUiState.Success? = null
        repeat(20) {
            val v = vm.uiState.value
            if (v is CalendarUiState.Success && v.dayDataMap.containsKey(targetDay)) {
                updatedState = v
                return@repeat
            }
            testScheduler.advanceUntilIdle()
            delay(10)
        }

    val updatedStatus = updatedState?.dayDataMap?.get(targetDay)?.status
    println("TEST DEBUG: updated keys = ${updatedState?.dayDataMap?.keys}")
    println("TEST DEBUG: updated state for $targetDay = ${updatedState?.dayDataMap?.get(targetDay)}")
    assertEquals("Expected COMPLETED_DAILY but was $updatedStatus", DayStatus.COMPLETED_DAILY, updatedStatus)
    }

    @Test
    fun `today summary updated from completed to in progress updates calendar status`() = runBlocking {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val targetDay = cal.timeInMillis // today start

        val completedSummary = DailySessionDaySummary(date = targetDay, itemCount = 2, completedCount = 2, totalReps = 10)
        val inProgressSummary = DailySessionDaySummary(date = targetDay, itemCount = 2, completedCount = 0, totalReps = 0)

        val summariesFlow = MutableStateFlow<List<DailySessionDaySummary>>(listOf(completedSummary))

        val fakeDailyDao = object : com.programminghut.pose_detection.data.dao.DailySessionDao {
            override suspend fun insertDailySession(session: com.programminghut.pose_detection.data.model.DailySession): Long = 1L
            override suspend fun insertSessionItems(items: List<com.programminghut.pose_detection.data.model.DailySessionItem>) {}
            override suspend fun insertSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem): Long = 1L
            override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long): com.programminghut.pose_detection.data.model.DailySession? = null
            override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySession?)
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySession>())
            override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long) = emptyList<Long>()
            override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long) = summariesFlow
            override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long): Int = 0
            override suspend fun getSessionItems(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override fun getSessionItemsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>())
            override suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean, actualReps: Int?, actualTime: Int?, completedAt: Long?, notes: String) {}
            override suspend fun getSessionItemById(itemId: Long) = null
            override suspend fun updateSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem) = 0
            override suspend fun updateSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSessionItems(sessionId: Long) {}
            override suspend fun deleteSessionItem(itemId: Long) {}
            override suspend fun getCompletedItemsCount(sessionId: Long): Int = 0
            override suspend fun getTotalItemsCount(sessionId: Long): Int = 0
            override fun getTotalCountForExercise(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(0)
            override suspend fun invalidateCountCacheForExercise(exerciseId: Long) = 0
            override suspend fun getItemsByParentWorkout(workoutItemId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {}
            override suspend fun getSessionById(sessionId: Long) = null
        }

        val fakeSessionDao = object : com.programminghut.pose_detection.data.dao.SessionDao { /* ...minimal stub...*/
            override suspend fun insertSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) = 1L
            override suspend fun insertSessions(sessions: List<com.programminghut.pose_detection.data.model.WorkoutSession>) = emptyList<Long>()
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getSessionById(sessionId: Long) = null
            override fun getSessionByIdFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.WorkoutSession?)
            override fun getSessionsByExerciseType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getSessionsByDateRange(startTime: Long, endTime: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getSessionsByTag(tag: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getTotalSessionsCount() = kotlinx.coroutines.flow.flowOf(0)
            override fun getSessionsCountByType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalRepsAllTime() = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalRepsByType(exerciseType: String) = kotlinx.coroutines.flow.flowOf(0)
            override fun getTotalWorkoutTimeSeconds() = kotlinx.coroutines.flow.flowOf(0)
            override fun getAverageFormScore() = kotlinx.coroutines.flow.flowOf(0f)
            override fun getRecentSessions(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getTodaySessions(dayStartTimestamp: Long, dayEndTimestamp: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getWeekSessions(weekStartTimestamp: Long, weekEndTimestamp: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getBestSessionByFormScore(exerciseType: String) = null
            override suspend fun getSessionWithMaxReps() = null
            override suspend fun updateSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) {}
            override suspend fun updateSessionNotes(sessionId: Long, notes: String) {}
            override suspend fun updateSessionTags(sessionId: Long, tags: String) {}
            override suspend fun updateSyncStatus(sessionId: Long, isSynced: Boolean) {}
            override suspend fun updateExportTimestamp(sessionId: Long, exportedAt: Long) {}
            override suspend fun deleteSession(session: com.programminghut.pose_detection.data.model.WorkoutSession) {}
            override suspend fun deleteSessionById(sessionId: Long) {}
            override suspend fun deleteAllSessions() {}
            override suspend fun deleteSessionsOlderThan(timestamp: Long) {}
            override fun getSessionsByType(sessionType: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getStreakAffectingSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun getSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>()
            override suspend fun hasSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long) = false
            override fun getRecoverySessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override fun getManualSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>())
            override suspend fun isDateAlreadyRecovered(recoveredDate: Long) = false
            override suspend fun getSessionsForCalendar(startTime: Long, endTime: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutSession>()
            override fun getCountBySessionType(sessionType: String) = kotlinx.coroutines.flow.flowOf(0)
        }

        val dummyRepDaoLocal = object : com.programminghut.pose_detection.data.dao.RepDao {
            override suspend fun insertRep(rep: com.programminghut.pose_detection.data.model.RepData): Long = 0L
            override suspend fun insertReps(reps: List<com.programminghut.pose_detection.data.model.RepData>): List<Long> = emptyList()
            override fun getRepsForSession(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.RepData>())
            override suspend fun getRepsForSessionOnce(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getRepById(repId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override fun getFlaggedRepsForSession(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.RepData>())
            override suspend fun getLowDepthReps(sessionId: Long, minDepthScore: Float) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getLowFormReps(sessionId: Long, minFormScore: Float) = emptyList<com.programminghut.pose_detection.data.model.RepData>()
            override suspend fun getRepCountForSession(sessionId: Long) = 0
            override suspend fun getAverageDepthScore(sessionId: Long) = 0f
            override suspend fun getAverageFormScore(sessionId: Long) = 0f
            override suspend fun getAverageSpeed(sessionId: Long) = 0f
            override suspend fun getBestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getWorstRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getFastestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getSlowestRepForSession(sessionId: Long) = null as com.programminghut.pose_detection.data.model.RepData?
            override suspend fun getFlaggedRepCount(sessionId: Long) = 0
            override suspend fun updateRep(rep: com.programminghut.pose_detection.data.model.RepData) {}
            override suspend fun updateReps(reps: List<com.programminghut.pose_detection.data.model.RepData>) {}
            override suspend fun flagRepForReview(repId: Long, isFlagged: Boolean) {}
            override suspend fun flagMultipleRepsForReview(repIds: List<Long>) {}
            override suspend fun deleteRep(rep: com.programminghut.pose_detection.data.model.RepData) {}
            override suspend fun deleteRepById(repId: Long) {}
            override suspend fun deleteRepsForSession(sessionId: Long) {}
            override suspend fun deleteAllReps() {}
        }

        val dailyRelationDaoStub = object : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
            override suspend fun getSessionWithItems(sessionId: Long) = null
            override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySessionWithItems?)
            override suspend fun getSessionItemsWithDetails(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>()
            override fun getSessionItemsWithDetailsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>())
            override suspend fun getSessionsHistory(limit: Int, offset: Int) = emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>()
            override fun getSessionsHistoryFlow() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>())
            override suspend fun getExerciseStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.ExerciseStats>()
            override suspend fun getWorkoutStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.WorkoutStats>()
        }
        val fakeExerciseDao = object : com.programminghut.pose_detection.data.dao.ExerciseDao { /* minimal */
            override suspend fun getExerciseById(exerciseId: Long) = null
            override suspend fun getExerciseByName(name: String) = null
            override suspend fun insertExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) = 1L
            override suspend fun updateExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) {}
            override fun getAllExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override suspend fun deleteAllCustomExercises() {}
            override suspend fun deleteExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) {}
            override suspend fun deleteExerciseById(exerciseId: Long) {}
            override suspend fun exerciseExistsByName(name: String) = false
            override suspend fun insertExercises(exercises: List<com.programminghut.pose_detection.data.model.Exercise>) {}
            override fun getExerciseByIdFlow(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.Exercise?)
            override fun getExercisesByType(type: com.programminghut.pose_detection.data.model.ExerciseType) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getPredefinedExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getCustomExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun searchExercisesByName(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override fun getExercisesByTag(tag: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
            override suspend fun getExerciseCount() = 0
            override suspend fun getCustomExerciseCount() = 0
            override fun getRecentlyModifiedExercises(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Exercise>())
        }
        val fakeWorkoutDao = object : com.programminghut.pose_detection.data.dao.WorkoutDao { /* minimal */
            override suspend fun insertWorkout(workout: com.programminghut.pose_detection.data.model.Workout) = 1L
            override suspend fun insertWorkoutExercises(exercises: List<com.programminghut.pose_detection.data.model.WorkoutExercise>) {}
            override suspend fun getWorkoutById(workoutId: Long) = null
            override suspend fun getWorkoutExercises(workoutId: Long) = emptyList<com.programminghut.pose_detection.data.model.WorkoutExercise>()
            override fun getAllWorkouts() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.Workout>())
            override fun getAllWorkoutsWithExercises() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutWithExercises>())
            override suspend fun getWorkoutWithExercises(workoutId: Long) = null
            override fun getWorkoutWithExercisesFlow(workoutId: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.WorkoutWithExercises?)
            override fun getWorkoutExercisesFlow(workoutId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.WorkoutExercise>())
            override suspend fun deleteWorkout(workout: com.programminghut.pose_detection.data.model.Workout) {}
            override suspend fun deleteWorkoutById(workoutId: Long) {}
            override suspend fun deleteWorkoutExercises(workoutId: Long) {}
            override suspend fun updateWorkout(workout: com.programminghut.pose_detection.data.model.Workout) {}
        }

        val sessionRepo = SessionRepository(fakeSessionDao, dummyRepDaoLocal)
        val dailyRepo = DailySessionRepository(fakeDailyDao, dailyRelationDaoStub, fakeExerciseDao, fakeWorkoutDao)

        val vm = CalendarViewModel(sessionRepo, dailyRepo)

        // initial emission
        testScheduler.advanceUntilIdle()
        // Ensure ViewModel picks up current DAO state deterministically in test
        dailyRepo.triggerSessionUpdateForTests()
        testScheduler.advanceUntilIdle()

        var initial: CalendarUiState.Success? = null
        repeat(10) {
            val v = vm.uiState.value
            if (v is CalendarUiState.Success && v.dayDataMap.containsKey(targetDay)) {
                initial = v
                return@repeat
            }
            testScheduler.advanceUntilIdle()
            delay(10)
        }

        // Should be COMPLETED (today with all completed)
        assertEquals(DayStatus.COMPLETED, initial?.dayDataMap?.get(targetDay)?.status)

        // Now emit an in-progress summary (completedCount < itemCount)
        summariesFlow.value = listOf(inProgressSummary)
    // Force repository-level update trigger so merged flow emits in test environment
    dailyRepo.triggerSessionUpdateForTests()

        testScheduler.advanceUntilIdle()

        var updated: CalendarUiState.Success? = null
        repeat(10) {
            val v = vm.uiState.value
            if (v is CalendarUiState.Success && v.dayDataMap.containsKey(targetDay)) {
                updated = v
                return@repeat
            }
            testScheduler.advanceUntilIdle()
            delay(10)
        }

    val finalStatus = updated?.dayDataMap?.get(targetDay)?.status
    assertEquals("Expected IN_PROGRESS but was $finalStatus", DayStatus.IN_PROGRESS, finalStatus)
    }
}
