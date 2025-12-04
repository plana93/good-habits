package com.programminghut.pose_detection.effects

data class GlowDotsFilterConfig(
    val glowRadius: Float = 12f,
    val glowColor: Int = 0xFFFFFF00.toInt(),
    val enabled: Boolean = true
)