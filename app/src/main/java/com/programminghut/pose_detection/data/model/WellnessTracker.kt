package com.programminghut.pose_detection.data.model

import androidx.room.*
import com.google.gson.annotations.SerializedName

/**
 * Represents the response type for a wellness tracker
 */
enum class TrackerResponseType {
    @SerializedName("RATING_5")
    RATING_5,           // 0-5 rating scale
    
    @SerializedName("BOOLEAN")
    BOOLEAN,            // Yes/No (true/false)
    
    @SerializedName("EMOTION_SET")
    EMOTION_SET,        // Multiple emotion options
    
    @SerializedName("TEXT_NOTE")
    TEXT_NOTE          // Free text entry
}

/**
 * Custom emotion for EMOTION_SET type trackers
 */
data class CustomEmotion(
    @SerializedName("emotion")
    val emotion: String,
    
    @SerializedName("emoticon")
    val emoticon: String,
    
    @SerializedName("label")
    val label: String
)

/**
 * Template for a wellness tracker (loaded from JSON)
 */
data class WellnessTrackerTemplate(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("responseType")
    val responseType: TrackerResponseType,
    
    @SerializedName("iconName")
    val iconName: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("emoticons")
    val emoticons: List<String>? = null,
    
    @SerializedName("labels")
    val labels: List<String>? = null,
    
    @SerializedName("customEmotions")
    val customEmotions: List<CustomEmotion>? = null,
    
    @SerializedName("invertedRating")
    val invertedRating: Boolean = false,  // For trackers where lower is better (e.g., stress)
    
    @SerializedName("isPredefined")
    val isPredefined: Boolean = true
)

/**
 * Category metadata
 */
data class TrackerCategory(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("icon")
    val icon: String,
    
    @SerializedName("color")
    val color: String
)

/**
 * Root JSON structure
 */
data class WellnessTrackerData(
    @SerializedName("version")
    val version: Int,
    
    @SerializedName("lastUpdated")
    val lastUpdated: String,
    
    @SerializedName("trackers")
    val trackers: List<WellnessTrackerTemplate>,
    
    @SerializedName("categories")
    val categories: List<TrackerCategory>
)

/**
 * User's response to a wellness tracker
 * This is stored in the database as part of DailySessionItem
 */
data class TrackerResponse(
    val trackerId: Int,
    val trackerName: String,
    val responseType: TrackerResponseType,
    
    // Response data (only one will be populated based on responseType)
    val ratingValue: Int? = null,          // For RATING_5 (0-5)
    val booleanValue: Boolean? = null,      // For BOOLEAN
    val selectedEmotion: String? = null,    // For EMOTION_SET
    val textNote: String? = null,           // For TEXT_NOTE or additional notes
    
    // ✅ Date tracking
    val timestamp: Long = System.currentTimeMillis(),      // When user entered this data
    val referenceDate: Long = System.currentTimeMillis()   // Which day this refers to (can be past)
) {
    /**
     * Calculate how many days ago the reference date is from when it was entered
     */
    fun getDaysAgo(): Int {
        val msPerDay = 24 * 60 * 60 * 1000
        val referenceDayStart = (referenceDate / msPerDay) * msPerDay
        val timestampDayStart = (timestamp / msPerDay) * msPerDay
        return ((timestampDayStart - referenceDayStart) / msPerDay).toInt()
    }
    
    /**
     * Get a human-readable description of when this was referenced
     * e.g., "Today", "Yesterday", "3 days ago"
     */
    fun getReferenceDateDescription(): String {
        val daysAgo = getDaysAgo()
        return when (daysAgo) {
            0 -> "Today"
            1 -> "Yesterday"
            2 -> "2 days ago"
            3 -> "3 days ago"
            else -> "$daysAgo days ago"
        }
    }
    
    /**
     * Convert to JSON string for storage in database
     */
    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }
    
    companion object {
        /**
         * Parse from JSON string stored in database
         */
        fun fromJson(json: String): TrackerResponse? {
            return try {
                com.google.gson.Gson().fromJson(json, TrackerResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
