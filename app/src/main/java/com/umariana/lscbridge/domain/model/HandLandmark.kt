package com.umariana.lscbridge.domain.model

data class HandLandmark(
    val x: Float,
    val y: Float,
    val z: Float
)

data class HandLandmarks(
    val landmarks: List<HandLandmark>,
    val handedness: String
)
