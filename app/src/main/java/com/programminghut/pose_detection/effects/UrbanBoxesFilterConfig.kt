package com.programminghut.pose_detection.effects

data class UrbanBoxesFilterConfig(
    val boxSize: Float = 48f,
    val borderColor: Int = 0xFFFFFFFF.toInt(),
    val enabled: Boolean = true
)