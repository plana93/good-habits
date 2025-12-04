package com.programminghut.pose_detection.effects

data class BlurFilterConfig(
    val radius: Float = 8f,
    val type: String = "gaussian",
    val enabled: Boolean = true
)