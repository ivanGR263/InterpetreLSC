package com.umariana.lscbridge.domain.model

data class GestureResult(
    val label: String,
    val confidence: Float,
    val timestamp: Long
)
