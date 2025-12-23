package com.programminghut.pose_detection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.programminghut.pose_detection.data.dao.DailySessionDao
import com.programminghut.pose_detection.data.dao.DailySessionRelationDao
import com.programminghut.pose_detection.data.dao.ExerciseDao
import com.programminghut.pose_detection.data.dao.RepDao
import com.programminghut.pose_detection.data.dao.SessionDao
import com.programminghut.pose_detection.data.dao.WorkoutDao
import com.programminghut.pose_detection.data.model.DailySession
import com.programminghut.pose_detection.data.model.DailySessionItem
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.ExerciseTypeConverters
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.Workout
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.model.WorkoutSession

/**
 * Room Database for Good Habits App
 * 
 * This is the main database class that holds the database holder and serves
 * as the main access point for the underlying connection to the app's data.
 * 
 * Version 1: Initial database with WorkoutSession and RepData tables
 * Version 2: Phase 4 - Added sessionType, recoveredDate, affectsStreak fields
 * Version 3: Phase 6 - Added Exercise table for multi-exercise tracking
 * Version 4: Phase 6 - Added exerciseName field to WorkoutSession
 * Version 5: Semplificazione - Rimossi keypoints/rules da Exercise, aggiunte Workout entities
 * Version 6: Modularità - Aggiunte DailySession entities per sessioni giornaliere flessibili
 */
@Database(
    entities = [
        WorkoutSession::class,
        RepData::class,
        Exercise::class,        // Semplificato: tracking manuale
        Workout::class,         // Nuova: allenamenti composti
        WorkoutExercise::class, // Nuova: junction table
        DailySession::class,    // Nuova: sessioni giornaliere
        DailySessionItem::class // Nuova: elementi nelle sessioni
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(ExerciseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Get the SessionDao to access workout_sessions table
     */
    abstract fun sessionDao(): SessionDao
    
    /**
     * Get the RepDao to access rep_data table
     */
    abstract fun repDao(): RepDao
    
    /**
     * Get the ExerciseDao to access exercises table
     */
    abstract fun exerciseDao(): ExerciseDao
    
    /**
     * Get the WorkoutDao to access workouts and workout_exercises tables
     */
    abstract fun workoutDao(): WorkoutDao
    
    /**
     * Get the DailySessionDao to access daily_sessions table
     */
    abstract fun dailySessionDao(): DailySessionDao
    
    /**
     * Get the DailySessionRelationDao for complex queries with relations
     */
    abstract fun dailySessionRelationDao(): DailySessionRelationDao
    
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)  // Phase 6: Add migration 3->4
                    
                    // For development only - destroys and rebuilds database on version changes
                    // Remove this in production and use proper migrations
                    .fallbackToDestructiveMigration()
                    
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
        
        /**
         * Migration from version 2 to 3
         * Phase 6: Multi-Exercise Tracking System
         * 
         * Adds exercises table for storing exercise definitions and rules
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create exercises table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercises (
                        exerciseId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        startPositionKeypoints TEXT NOT NULL,
                        endPositionKeypoints TEXT NOT NULL,
                        rules TEXT NOT NULL,
                        depthTolerance REAL NOT NULL DEFAULT 0.05,
                        symmetryTolerance REAL NOT NULL DEFAULT 0.1,
                        createdAt INTEGER NOT NULL,
                        modifiedAt INTEGER NOT NULL,
                        isCustom INTEGER NOT NULL DEFAULT 0,
                        imageUri TEXT,
                        tags TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                
                // Create index on exercise name for faster searches
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)"
                )
                
                // Create index on exercise type
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercises_type ON exercises(type)"
                )
            }
        }
        
        /**
         * Migration from version 3 to 4
         * Adds exerciseName field to WorkoutSession table
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add exerciseName column to workout_sessions table with temporary default
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN exerciseName TEXT NOT NULL DEFAULT ''"
                )
                
                // Update exerciseName to match exerciseType for all existing records
                database.execSQL(
                    "UPDATE workout_sessions SET exerciseName = exerciseType WHERE exerciseName = ''"
                )
            }
        }
    }
}
