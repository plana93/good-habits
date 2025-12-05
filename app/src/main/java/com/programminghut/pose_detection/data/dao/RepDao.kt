package com.programminghut.pose_detection.data.dao

import androidx.room.*
import com.programminghut.pose_detection.data.model.RepData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for RepData entities
 * 
 * Provides methods to interact with rep_data table.
 * Each rep belongs to a WorkoutSession via foreign key.
 */
@Dao
interface RepDao {
    
    // ============================================================
    // CREATE
    // ============================================================
    
    /**
     * Insert a single rep
     * @return The ID of the inserted rep
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRep(rep: RepData): Long
    
    /**
     * Insert multiple reps at once (batch insert for performance)
     * @return List of inserted rep IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReps(reps: List<RepData>): List<Long>
    
    
    // ============================================================
    // READ - Basic Queries
    // ============================================================
    
    /**
     * Get all reps for a specific session, ordered by rep number
     */
    @Query("SELECT * FROM rep_data WHERE sessionId = :sessionId ORDER BY repNumber ASC")
    fun getRepsForSession(sessionId: Long): Flow<List<RepData>>
    
    /**
     * Get all reps for a specific session (suspend version for one-time reads)
     */
    @Query("SELECT * FROM rep_data WHERE sessionId = :sessionId ORDER BY repNumber ASC")
    suspend fun getRepsForSessionOnce(sessionId: Long): List<RepData>
    
    /**
     * Get a specific rep by ID
     */
    @Query("SELECT * FROM rep_data WHERE repId = :repId")
    suspend fun getRepById(repId: Long): RepData?
    
    
    // ============================================================
    // READ - Filtered Queries
    // ============================================================
    
    /**
     * Get reps flagged for review in a session
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId AND isFlaggedForReview = 1 
        ORDER BY repNumber ASC
    """)
    fun getFlaggedRepsForSession(sessionId: Long): Flow<List<RepData>>
    
    /**
     * Get reps with depth score below threshold
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId AND depthScore < :minDepthScore 
        ORDER BY repNumber ASC
    """)
    suspend fun getLowDepthReps(sessionId: Long, minDepthScore: Float): List<RepData>
    
    /**
     * Get reps with form score below threshold
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId AND formScore < :minFormScore 
        ORDER BY repNumber ASC
    """)
    suspend fun getLowFormReps(sessionId: Long, minFormScore: Float): List<RepData>
    
    
    // ============================================================
    // READ - Analytics & Statistics
    // ============================================================
    
    /**
     * Get count of reps for a session
     */
    @Query("SELECT COUNT(*) FROM rep_data WHERE sessionId = :sessionId")
    suspend fun getRepCountForSession(sessionId: Long): Int
    
    /**
     * Get average depth score for a session
     */
    @Query("SELECT AVG(depthScore) FROM rep_data WHERE sessionId = :sessionId")
    suspend fun getAverageDepthScore(sessionId: Long): Float?
    
    /**
     * Get average form score for a session
     */
    @Query("SELECT AVG(formScore) FROM rep_data WHERE sessionId = :sessionId")
    suspend fun getAverageFormScore(sessionId: Long): Float?
    
    /**
     * Get average speed for a session
     */
    @Query("SELECT AVG(speed) FROM rep_data WHERE sessionId = :sessionId")
    suspend fun getAverageSpeed(sessionId: Long): Float?
    
    /**
     * Get best rep (highest form score) for a session
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId 
        ORDER BY formScore DESC 
        LIMIT 1
    """)
    suspend fun getBestRepForSession(sessionId: Long): RepData?
    
    /**
     * Get worst rep (lowest form score) for a session
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId 
        ORDER BY formScore ASC 
        LIMIT 1
    """)
    suspend fun getWorstRepForSession(sessionId: Long): RepData?
    
    /**
     * Get fastest rep for a session
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId 
        ORDER BY speed ASC 
        LIMIT 1
    """)
    suspend fun getFastestRepForSession(sessionId: Long): RepData?
    
    /**
     * Get slowest rep for a session
     */
    @Query("""
        SELECT * FROM rep_data 
        WHERE sessionId = :sessionId 
        ORDER BY speed DESC 
        LIMIT 1
    """)
    suspend fun getSlowestRepForSession(sessionId: Long): RepData?
    
    /**
     * Get count of flagged reps for a session
     */
    @Query("SELECT COUNT(*) FROM rep_data WHERE sessionId = :sessionId AND isFlaggedForReview = 1")
    suspend fun getFlaggedRepCount(sessionId: Long): Int
    
    
    // ============================================================
    // UPDATE
    // ============================================================
    
    /**
     * Update an existing rep
     */
    @Update
    suspend fun updateRep(rep: RepData)
    
    /**
     * Update multiple reps at once
     */
    @Update
    suspend fun updateReps(reps: List<RepData>)
    
    /**
     * Flag a rep for review
     */
    @Query("UPDATE rep_data SET isFlaggedForReview = :isFlagged WHERE repId = :repId")
    suspend fun flagRepForReview(repId: Long, isFlagged: Boolean)
    
    /**
     * Flag multiple reps for review
     */
    @Query("UPDATE rep_data SET isFlaggedForReview = 1 WHERE repId IN (:repIds)")
    suspend fun flagMultipleRepsForReview(repIds: List<Long>)
    
    
    // ============================================================
    // DELETE
    // ============================================================
    
    /**
     * Delete a specific rep
     */
    @Delete
    suspend fun deleteRep(rep: RepData)
    
    /**
     * Delete a rep by ID
     */
    @Query("DELETE FROM rep_data WHERE repId = :repId")
    suspend fun deleteRepById(repId: Long)
    
    /**
     * Delete all reps for a specific session
     * Note: This is handled automatically by cascade delete in foreign key
     */
    @Query("DELETE FROM rep_data WHERE sessionId = :sessionId")
    suspend fun deleteRepsForSession(sessionId: Long)
    
    /**
     * Delete all reps (use with caution!)
     */
    @Query("DELETE FROM rep_data")
    suspend fun deleteAllReps()
}
