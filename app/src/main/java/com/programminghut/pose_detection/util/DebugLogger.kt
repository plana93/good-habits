package com.programminghut.pose_detection.util

import android.util.Log
import com.programminghut.pose_detection.BuildConfig

/**
 * 🎯 Smart logger che disabilita automaticamente i log in build RELEASE
 * Usa questo invece di Log.d() diretto per migliore performance
 */
object DebugLogger {
    
    /**
     * Log di debug - DISABILITATO in release builds
     */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log di error - SEMPRE ABILITATO
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
    
    /**
     * Log di warning - SEMPRE ABILITATO
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    /**
     * Log di info - DISABILITATO in release builds
     */
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
}
