package com.programminghut.pose_detection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
 */
@Database(
    entities = [
        WorkoutSession::class,
        RepData::class
    ],
    version = 1,
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
                    // Add migrations here when database schema changes
                    // .addMigrations(MIGRATION_1_2)
                    
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
        
        // Future migrations will be added here as the database schema evolves
        // Example:
        // private val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         // Migration logic here
        //         // e.g., database.execSQL("ALTER TABLE workout_sessions ADD COLUMN newColumn TEXT")
        //     }
        // }
    }
}
