package com.programminghut.pose_detection.util

import android.util.Log
import com.programminghut.pose_detection.BuildConfig

/**
 * Simple runtime-configurable logging helpers
 * Default enabled in debug builds, can be toggled in runtime for finer control
 */
object Logging {
    // Toggleable flags - default to BuildConfig.DEBUG so production is quiet
    var TODAY_DEBUG: Boolean = BuildConfig.DEBUG
    var CALENDAR_DEBUG: Boolean = BuildConfig.DEBUG
    var RECOVERY_DEBUG: Boolean = BuildConfig.DEBUG
}

fun todayDebug(message: String) {
    if (Logging.TODAY_DEBUG) Log.d("TODAY_DEBUG", message)
}

fun calendarDebug(message: String) {
    if (Logging.CALENDAR_DEBUG) Log.d("CALENDAR_DEBUG", message)
}

fun recoveryDebug(message: String) {
    if (Logging.RECOVERY_DEBUG) Log.d("RECOVERY_DEBUG", message)
}
