package com.cesmag.nexing.ml

import android.content.Context
import com.cesmag.nexing.domain.model.GestureResult
import com.cesmag.nexing.domain.model.HandLandmarks
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class TFLiteGestureClassifier {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    fun initialize(context: Context): Boolean {
        return try {
            val modelBuffer = FileUtil.loadMappedFile(context, "gesture_classifier.tflite")
            interpreter = Interpreter(modelBuffer)
            labels = runCatching {
                FileUtil.loadLabels(context, "gesture_labels.txt")
            }.getOrDefault(emptyList())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun classify(handLandmarks: HandLandmarks?): GestureResult {
        val model = interpreter
        if (model == null || handLandmarks == null || handLandmarks.landmarks.isEmpty()) {
            return GestureResult(
                label = "Sin detección",
                confidence = 0f,
                timestamp = System.currentTimeMillis()
            )
        }

        val input = Array(1) { FloatArray(63) }
        handLandmarks.landmarks.take(21).forEachIndexed { index, landmark ->
            val baseIndex = index * 3
            input[0][baseIndex] = landmark.x
            input[0][baseIndex + 1] = landmark.y
            input[0][baseIndex + 2] = landmark.z
        }

        val outputClasses = if (labels.isNotEmpty()) labels.size else 4
        val output = Array(1) { FloatArray(outputClasses) }
        model.run(input, output)

        val probabilities = output[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities.getOrElse(maxIndex) { 0f }
        val label = labels.getOrElse(maxIndex) { "Gesto_$maxIndex" }

        return GestureResult(label = label, confidence = confidence, timestamp = System.currentTimeMillis())
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
