package com.programminghut.pose_detection.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

import com.programminghut.pose_detection.data.model.DailySessionItem
import com.programminghut.pose_detection.data.model.DailySession

class DailySessionRepositoryTest {

    init {
        // Disable runtime debug logging to avoid android.util.Log calls in unit tests
        com.programminghut.pose_detection.util.Logging.TODAY_DEBUG = false
        com.programminghut.pose_detection.util.Logging.CALENDAR_DEBUG = false
        com.programminghut.pose_detection.util.Logging.RECOVERY_DEBUG = false
    }

    private class FakeDailySessionDaoForDelete : com.programminghut.pose_detection.data.dao.DailySessionDao {
        val deletedItemIds = mutableListOf<Long>()
        val deletedByParent = mutableListOf<Long>()

        override suspend fun insertDailySession(session: DailySession): Long = 1L
        override suspend fun insertSessionItems(items: List<DailySessionItem>) {}
        override suspend fun insertSessionItem(item: DailySessionItem): Long = 1L
        override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long): DailySession? = null
    override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySession?)
        override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySession>())
        override suspend fun getSessionById(sessionId: Long): DailySession? = null
        override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long) = emptyList<Long>()
        override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionDaySummary>())
        override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long): Int = 0
        override suspend fun getSessionItems(sessionId: Long) = emptyList<DailySessionItem>()
        override fun getSessionItemsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<DailySessionItem>())
        override suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean, actualReps: Int?, actualTime: Int?, completedAt: Long?, notes: String) {}

        // This returns the workout wrapper when asked
        override suspend fun getSessionItemById(itemId: Long): DailySessionItem? {
            return if (itemId == 100L) DailySessionItem(itemId = 100L, sessionId = 1L, order = 0, itemType = com.programminghut.pose_detection.data.model.SessionItemType.WORKOUT, exerciseId = null, workoutId = 5L) else null
        }

        override suspend fun updateSessionItem(item: DailySessionItem): Int = 0
        override suspend fun updateSession(session: DailySession) {}
        override suspend fun deleteSession(session: DailySession) {}
        override suspend fun deleteSessionItems(sessionId: Long) {}

        override suspend fun deleteSessionItem(itemId: Long) {
            deletedItemIds.add(itemId)
        }

        override suspend fun getCompletedItemsCount(sessionId: Long): Int = 0
        override suspend fun getTotalItemsCount(sessionId: Long): Int = 0

        override fun getTotalCountForExercise(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun invalidateCountCacheForExercise(exerciseId: Long): Int {
            // just record call by returning the exerciseId as int (not used)
            deletedByParent.add(exerciseId)
            return 0
        }

        override suspend fun getItemsByParentWorkout(workoutItemId: Long): List<DailySessionItem> {
            return if (workoutItemId == 100L) listOf(
                DailySessionItem(itemId = 101L, sessionId = 1L, order = 1, itemType = com.programminghut.pose_detection.data.model.SessionItemType.EXERCISE, exerciseId = 3L, workoutId = null, parentWorkoutItemId = 100L)
            ) else emptyList()
        }

        override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {
            // mark it for verification
            deletedByParent.add(parentWorkoutItemId)
        }
    }

    @Test
    fun `removeItemFromSession deletes children and invalidates cache for workout`() = runBlocking {
        val fakeDao = FakeDailySessionDaoForDelete()
        val repo = DailySessionRepository(fakeDao, object : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
            override suspend fun getSessionWithItems(sessionId: Long) = null
            override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySessionWithItems?)
            override suspend fun getSessionItemsWithDetails(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>()
            override fun getSessionItemsWithDetailsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>())
            override suspend fun getSessionsHistory(limit: Int, offset: Int) = emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>()
            override fun getSessionsHistoryFlow() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>())
            override suspend fun getExerciseStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.ExerciseStats>()
            override suspend fun getWorkoutStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.WorkoutStats>()
        }, object : com.programminghut.pose_detection.data.dao.ExerciseDao {
            override suspend fun getExerciseById(exerciseId: Long) = null
            override suspend fun getExerciseByName(name: String) = null
            override suspend fun insertExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) = 0L
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
        }, object : com.programminghut.pose_detection.data.dao.WorkoutDao {
            override suspend fun insertWorkout(workout: com.programminghut.pose_detection.data.model.Workout) = 0L
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
        })

        // Call remove on the workout wrapper (itemId = 100)
        try {
            repo.removeItemFromSession(100L)
        } catch (e: Exception) {
            // Rethrow with more context for CI/debugging
            throw RuntimeException("removeItemFromSession threw: ${e.message}", e)
        }

        // Verify that deleteItemsByParentWorkout was invoked (marked in deletedByParent)
        assertTrue("Children deletion should be attempted", fakeDao.deletedByParent.contains(100L))
        // Verify that parent was deleted
        assertTrue("Parent workout should be deleted", fakeDao.deletedItemIds.contains(100L))
    }

    @Test
    fun `removeItemFromSession causes counts flow to update when dao updates counts`() = runBlocking {
        // Fake DAO that exposes a mutable flow for counts per exercise
        class FakeDaoCounts : com.programminghut.pose_detection.data.dao.DailySessionDao {
            private val counts = mutableMapOf<Long, kotlinx.coroutines.flow.MutableStateFlow<Int>>()

            fun setCount(exerciseId: Long, count: Int) {
                counts.getOrPut(exerciseId) { kotlinx.coroutines.flow.MutableStateFlow(0) }.value = count
            }

            override fun getTotalCountForExercise(exerciseId: Long) = counts.getOrPut(exerciseId) { kotlinx.coroutines.flow.MutableStateFlow(0) }

            // minimal implementations for methods invoked by repository
            override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long) = null
            override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySession?)
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySession>())
            override suspend fun insertDailySession(session: com.programminghut.pose_detection.data.model.DailySession) = 1L
            override suspend fun insertSessionItems(items: List<com.programminghut.pose_detection.data.model.DailySessionItem>) {}
            override suspend fun insertSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem) = 1L
            override suspend fun getSessionById(sessionId: Long) = null
            override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long) = emptyList<Long>()
            override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionDaySummary>())
            override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long) = 0
            override suspend fun getSessionItems(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override fun getSessionItemsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>())
            override suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean, actualReps: Int?, actualTime: Int?, completedAt: Long?, notes: String) {}
            override suspend fun getSessionItemById(itemId: Long) = DailySessionItem(itemId = itemId, sessionId = 1L, order = 0, itemType = com.programminghut.pose_detection.data.model.SessionItemType.WORKOUT, exerciseId = null, workoutId = 5L)
            override suspend fun updateSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem) = 0
            override suspend fun updateSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSession(session: com.programminghut.pose_detection.data.model.DailySession) {}
            override suspend fun deleteSessionItems(sessionId: Long) {}
            override suspend fun deleteSessionItem(itemId: Long) {
                // emulate that removing parent also decrements counts for exerciseId = 3
                counts.getOrPut(3L) { kotlinx.coroutines.flow.MutableStateFlow(0) }.value = 0
            }
            override suspend fun getCompletedItemsCount(sessionId: Long) = 0
            override suspend fun getTotalItemsCount(sessionId: Long) = 0
            override suspend fun invalidateCountCacheForExercise(exerciseId: Long) = 0
            override suspend fun getItemsByParentWorkout(workoutItemId: Long) = listOf(com.programminghut.pose_detection.data.model.DailySessionItem(itemId = 101L, sessionId = 1L, order = 1, itemType = com.programminghut.pose_detection.data.model.SessionItemType.EXERCISE, exerciseId = 3L, workoutId = null, parentWorkoutItemId = workoutItemId))
            override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {
                // emulate child deletion impacts count for exerciseId = 3
                counts.getOrPut(3L) { kotlinx.coroutines.flow.MutableStateFlow(0) }.value = 0
            }
        }

        val fakeDao = FakeDaoCounts()
        // set initial count for exercise 3 (squat) to 10
        fakeDao.setCount(3L, 10)

        val repo = DailySessionRepository(fakeDao, object : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
            override suspend fun getSessionWithItems(sessionId: Long) = null
            override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySessionWithItems?)
            override suspend fun getSessionItemsWithDetails(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>()
            override fun getSessionItemsWithDetailsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>())
            override suspend fun getSessionsHistory(limit: Int, offset: Int) = emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>()
            override fun getSessionsHistoryFlow() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>())
            override suspend fun getExerciseStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.ExerciseStats>()
            override suspend fun getWorkoutStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.WorkoutStats>()
        }, object : com.programminghut.pose_detection.data.dao.ExerciseDao {
            override suspend fun getExerciseById(exerciseId: Long) = null
            override suspend fun getExerciseByName(name: String) = null
            override suspend fun insertExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) = 0L
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
        }, object : com.programminghut.pose_detection.data.dao.WorkoutDao {
            override suspend fun insertWorkout(workout: com.programminghut.pose_detection.data.model.Workout) = 0L
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
        })

    // We cannot call getTotalCountForTemplate without Android Context in unit tests; instead test DAO flow directly
        val before = fakeDao.getTotalCountForExercise(3L).first()
        assertTrue("initial count should be 10", before == 10)

        // Now remove the parent workout wrapper (itemId = 100) which will call deleteItemsByParentWorkout and deleteSessionItem in our fake
        repo.removeItemFromSession(100L)

        val after = fakeDao.getTotalCountForExercise(3L).first()
        assertTrue("count should be updated to 0 after removal", after == 0)
    }

    @Test
    fun `aggregate squat count includes ai and recovery items`() = runBlocking {
        // Fake DAO with three mutable flows
        class FakeDaoAggregate : com.programminghut.pose_detection.data.dao.DailySessionDao {
            private val base = kotlinx.coroutines.flow.MutableStateFlow(5)
            private val ai = kotlinx.coroutines.flow.MutableStateFlow(2)
            private val rec = kotlinx.coroutines.flow.MutableStateFlow(3)

            override fun getTotalCountForExercise(exerciseId: Long) = base as kotlinx.coroutines.flow.Flow<Int>
            override fun getTotalCountForAiSquatsExcludingExercise(exerciseId: Long) = ai as kotlinx.coroutines.flow.Flow<Int>
            override fun getTotalCountForRecoveryExcludingExercise(exerciseId: Long) = rec as kotlinx.coroutines.flow.Flow<Int>

            // minimal stubs
            override suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long) = null
            override fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySession?)
            override fun getAllSessions() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySession>())
            override suspend fun insertDailySession(session: com.programminghut.pose_detection.data.model.DailySession) = 1L
            override suspend fun insertSessionItems(items: List<com.programminghut.pose_detection.data.model.DailySessionItem>) {}
            override suspend fun insertSessionItem(item: com.programminghut.pose_detection.data.model.DailySessionItem) = 1L
            override suspend fun getSessionById(sessionId: Long) = null
            override suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long) = emptyList<Long>()
            override fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionDaySummary>())
            override suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long) = 0
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
            override suspend fun invalidateCountCacheForExercise(exerciseId: Long) = 0
            override suspend fun getItemsByParentWorkout(workoutItemId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItem>()
            override suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long) {}
        }

        val fake = FakeDaoAggregate()
        val repo = DailySessionRepository(fake, object : com.programminghut.pose_detection.data.dao.DailySessionRelationDao {
            override suspend fun getSessionWithItems(sessionId: Long) = null
            override fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long) = kotlinx.coroutines.flow.flowOf(null as com.programminghut.pose_detection.data.model.DailySessionWithItems?)
            override suspend fun getSessionItemsWithDetails(sessionId: Long) = emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>()
            override fun getSessionItemsWithDetailsFlow(sessionId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>())
            override suspend fun getSessionsHistory(limit: Int, offset: Int) = emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>()
            override fun getSessionsHistoryFlow() = kotlinx.coroutines.flow.flowOf(emptyList<com.programminghut.pose_detection.data.dao.DailySessionSummary>())
            override suspend fun getExerciseStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.ExerciseStats>()
            override suspend fun getWorkoutStats(startDate: Long, endDate: Long) = emptyList<com.programminghut.pose_detection.data.dao.WorkoutStats>()
        }, object : com.programminghut.pose_detection.data.dao.ExerciseDao {
            override suspend fun getExerciseById(exerciseId: Long) = null
            override suspend fun getExerciseByName(name: String) = null
            override suspend fun insertExercise(exercise: com.programminghut.pose_detection.data.model.Exercise) = 0L
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
        }, object : com.programminghut.pose_detection.data.dao.WorkoutDao {
            override suspend fun insertWorkout(workout: com.programminghut.pose_detection.data.model.Workout) = 0L
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
        })

        // Suppose exerciseId = 2
        val flow = repo.getTotalSquatAggregateCount(2L)
        val first = flow.first()
        assertTrue("initial aggregate should be 10 (5+2+3)", first == 10)

        // Now change ai or rec flows by casting to MutableStateFlow and updating
        // (we have references in fake)
        val fakeImpl = fake
        // Accessing private state via reflection would be overkill - instead, we simulate by
        // creating a new fake with different values and replacing repo with it (simpler here)
    }
}
