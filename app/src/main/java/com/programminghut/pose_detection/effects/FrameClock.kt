package com.programminghut.pose_detection.effects

/**
 * Global frame clock to drive time-based animations deterministically during export.
 * Call setTimeMs(...) once per frame before rendering.
 */
object FrameClock {
    @Volatile
    var timeMs: Long = System.currentTimeMillis()

    /**
     * Set the current frame time in milliseconds. Named differently than the property
     * setter to avoid a JVM signature clash with the generated `setTimeMs` for the
     * `timeMs` var.
     */
    @Synchronized
    fun setFrameTimeMs(ms: Long) {
        timeMs = ms
    }
}
