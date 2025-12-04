package com.programminghut.pose_detection.effects

data class SkeletonFilterConfig(
    val lineWidth: Float = 4f,
    val lineColor: Int = 0xFF00FF00.toInt(),
    val enabled: Boolean = true
)