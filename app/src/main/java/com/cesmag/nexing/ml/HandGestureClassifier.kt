package com.cesmag.nexing.ml

import com.cesmag.nexing.domain.model.GestureResult

class HandGestureClassifier {
    fun classify(): GestureResult {
        return GestureResult(
            label = "Sin gesto",
            confidence = 0f,
            timestamp = System.currentTimeMillis()
        )
    }
}
