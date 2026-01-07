package com.programminghut.pose_detection.data.manager

import android.content.Context
import com.google.gson.Gson
import com.programminghut.pose_detection.data.model.WellnessTrackerData
import com.programminghut.pose_detection.data.model.WellnessTrackerTemplate
import com.programminghut.pose_detection.data.model.TrackerCategory
import java.io.IOException

/**
 * Manager for loading and accessing Wellness Tracker templates from JSON
 * 
 * Similar to ExerciseFileManager but for wellness/mood tracking items
 */
class WellnessTrackerFileManager(private val context: Context) {
    
    private val gson = Gson()
    private var trackerData: WellnessTrackerData? = null
    
    companion object {
        private const val TRACKER_TEMPLATES_FILE = "wellness_tracker_templates.json"
        private const val TAG = "WellnessTrackerFileManager"
    }
    
    /**
     * Load wellness tracker templates from JSON file
     */
    fun loadTrackers(): WellnessTrackerData {
        if (trackerData != null) {
            return trackerData!!
        }
        
        return try {
            val jsonString = context.assets.open(TRACKER_TEMPLATES_FILE).bufferedReader().use {
                it.readText()
            }
            
            val data = gson.fromJson(jsonString, WellnessTrackerData::class.java)
            trackerData = data
            android.util.Log.d(TAG, "Loaded ${data.trackers.size} wellness trackers")
            data
            
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Error loading wellness trackers", e)
            // Return empty data on error
            WellnessTrackerData(
                version = 1,
                lastUpdated = "",
                trackers = emptyList(),
                categories = emptyList()
            )
        }
    }
    
    /**
     * Get all available wellness tracker templates
     */
    fun getAllTrackers(): List<WellnessTrackerTemplate> {
        return loadTrackers().trackers
    }
    
    /**
     * Get a specific tracker by ID
     */
    fun getTrackerById(id: Int): WellnessTrackerTemplate? {
        return loadTrackers().trackers.find { it.id == id }
    }
    
    /**
     * Get trackers by category
     */
    fun getTrackersByCategory(category: String): List<WellnessTrackerTemplate> {
        return loadTrackers().trackers.filter { it.category == category }
    }
    
    /**
     * Get all categories
     */
    fun getAllCategories(): List<TrackerCategory> {
        return loadTrackers().categories
    }
    
    /**
     * Get category by ID
     */
    fun getCategoryById(id: String): TrackerCategory? {
        return loadTrackers().categories.find { it.id == id }
    }
    
    /**
     * Search trackers by name
     */
    fun searchTrackers(query: String): List<WellnessTrackerTemplate> {
        val lowerQuery = query.lowercase()
        return loadTrackers().trackers.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery) ||
            it.category.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Clear cached data (useful for testing or reloading)
     */
    fun clearCache() {
        trackerData = null
    }
}
