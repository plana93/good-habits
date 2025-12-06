package com.programminghut.pose_detection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.programminghut.pose_detection.data.dao.RepDao
import com.programminghut.pose_detection.data.dao.SessionDao
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession

/**
 * Room Database for Good Habits App
 * 
 * This is the main database class that holds the database holder and serves
 * as the main access point for the underlying connection to the app's data.
 * 
 * Version 1: Initial database with WorkoutSession and RepData tables
 * Version 2: Phase 4 - Added sessionType, recoveredDate, affectsStreak fields
 */
@Database(
    entities = [
        WorkoutSession::class,
        RepData::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Get the SessionDao to access workout_sessions table
     */
    abstract fun sessionDao(): SessionDao
    
    /**
     * Get the RepDao to access rep_data table
     */
    abstract fun repDao(): RepDao
    
    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Database name
        private const val DATABASE_NAME = "good_habits_database"
        
        /**
         * Get database instance (singleton pattern)
         * 
         * If the INSTANCE is not null, then return it,
         * if it is, then create the database
         * 
         * @param context Application context
         * @return AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is not null, return it
            // Otherwise create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Add migrations
                    .addMigrations(MIGRATION_1_2)
                    
                    // For development only - destroys and rebuilds database on version changes
                    // Remove this in production and use proper migrations
                    // .fallbackToDestructiveMigration()
                    
                    .build()
                
                INSTANCE = instance
                // Return instance
                instance
            }
        }
        
        /**
         * Close the database instance
         * Useful for testing or when app is being destroyed
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
        
        /**
         * Migration from version 1 to 2
         * Phase 4: Session Recovery & Calendar
         * 
         * Adds new columns for session type tracking and recovery functionality:
         * - sessionType: Type of session (REAL_TIME, MANUAL, RECOVERY)
         * - recoveredDate: Date being recovered (for RECOVERY sessions)
         * - affectsStreak: Whether session counts for streak
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to workout_sessions table
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'REAL_TIME'"
                )
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN recoveredDate INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN affectsStreak INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
