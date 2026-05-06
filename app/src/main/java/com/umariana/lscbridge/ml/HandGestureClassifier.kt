package com.umariana.lscbridge.ml

import com.umariana.lscbridge.domain.model.GestureResult

class HandGestureClassifier {
    fun classify(): GestureResult {
        return GestureResult(
            label = "Sin gesto",
            confidence = 0f,
            timestamp = System.currentTimeMillis()
        )
    }
}
