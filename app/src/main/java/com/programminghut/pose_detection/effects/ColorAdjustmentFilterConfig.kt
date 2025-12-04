package com.programminghut.pose_detection.effects

data class ColorAdjustmentFilterConfig(
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val enabled: Boolean = true
)