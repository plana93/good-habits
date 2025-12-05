package com.programminghut.pose_detection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * WorkoutSession Entity
 * 
 * Represents a complete workout session with metadata and statistics.
 * Each session contains multiple reps tracked in the RepData table.
 */
@Entity(tableName = "workout_sessions")
@TypeConverters(Converters::class)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    
    // Timestamp and duration
    val startTime: Long,              // Unix timestamp in milliseconds
    val endTime: Long,                // Unix timestamp in milliseconds
    val durationSeconds: Int,         // Total duration in seconds
    
    // Session metadata
    val exerciseType: String,         // e.g., "SQUAT", "PUSH_UP", etc.
    val totalReps: Int,               // Total number of reps completed
    
    // Quality metrics (aggregated from reps)
    val avgDepthScore: Float,         // Average depth score (0.0-1.0)
    val avgFormScore: Float,          // Average form quality (0.0-1.0)
    val avgSpeed: Float,              // Average rep speed in seconds
    
    // Additional metadata
    val notes: String? = null,        // Optional user notes
    val tags: List<String> = emptyList(), // Tags like "morning", "warm-up", etc.
    val location: String? = null,     // Optional location name
    
    // Device and app info
    val appVersion: String,           // App version for data compatibility
    val deviceModel: String? = null,  // Device model for analytics
    
    // Sync and export status
    val isSynced: Boolean = false,    // Cloud sync status
    val exportedAt: Long? = null      // Last export timestamp
)

/**
 * RepData Entity
 * 
 * Represents a single repetition within a workout session.
 * Tracks detailed quality metrics for each rep.
 */
@Entity(
    tableName = "rep_data",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class RepData(
    @PrimaryKey(autoGenerate = true)
    val repId: Long = 0,
    
    // Foreign key to parent session
    val sessionId: Long,
    
    // Rep sequence
    val repNumber: Int,               // Rep number within session (1, 2, 3...)
    val timestamp: Long,              // Unix timestamp when rep was recorded
    
    // Quality metrics
    val depthScore: Float,            // Depth score (0.0-1.0)
    val formScore: Float,             // Form quality score (0.0-1.0)
    val speed: Float,                 // Time taken for this rep in seconds
    
    // Detailed pose data (optional, for advanced analysis)
    val keypoints: String? = null,    // JSON serialized keypoint data
    val confidence: Float,            // Overall pose detection confidence
    
    // Rep-specific flags
    val isFlaggedForReview: Boolean = false // Flag for poor form
)

/**
 * Type converters for Room Database
 * 
 * Handles conversion of complex types (Lists, etc.) to/from database-compatible types.
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
