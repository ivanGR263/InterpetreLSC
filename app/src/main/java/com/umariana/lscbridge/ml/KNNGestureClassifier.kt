package com.umariana.lscbridge.ml

import android.content.Context
import com.umariana.lscbridge.domain.model.GestureResult
import com.umariana.lscbridge.domain.model.HandLandmarks
import java.io.File
import kotlin.math.sqrt

class KNNGestureClassifier {
    private data class Sample(val label: String, val features: FloatArray)

    private var samples: List<Sample> = emptyList()
    private val K = 3

    fun initialize(context: Context): Boolean {
        return try {
            // 1. Intentar cargar desde Almacenamiento Interno (capturas nuevas)
            val internalFile = File(context.filesDir, "training/gestures_dataset.csv")
            val lines = if (internalFile.exists()) {
                internalFile.readLines()
            } else {
                // 2. Si no existe, intentar cargar desde Assets (el abecedario limpio que incluiremos)
                try {
                    context.assets.open("gestures_dataset.csv").bufferedReader().readLines()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (lines.isEmpty()) return false
            
            samples = lines
                .drop(1) // Header
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size < 64) return@mapNotNull null
                        val label = parts[0]
                        val features = parts.drop(1).map { it.toFloat() }.toFloatArray()
                        Sample(label, features)
                    } catch (e: Exception) {
                        null
                    }
                }
            samples.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun classify(handLandmarks: HandLandmarks?): GestureResult {
        if (samples.isEmpty() || handLandmarks == null || handLandmarks.landmarks.size < 21) {
            return GestureResult(
                label = if (samples.isEmpty()) "CSV no cargado" else "Buscando mano...",
                confidence = 0f,
                timestamp = System.currentTimeMillis()
            )
        }

        val inputFeatures = FloatArray(63)
        handLandmarks.landmarks.take(21).forEachIndexed { index, landmark ->
            inputFeatures[index * 3] = landmark.x
            inputFeatures[index * 3 + 1] = landmark.y
            inputFeatures[index * 3 + 2] = landmark.z
        }

        // Calcular distancias Euclidianas
        val distances = samples.map { sample ->
            sample.label to euclideanDistance(inputFeatures, sample.features)
        }.sortedBy { it.second }

        // Obtener los K vecinos más cercanos
        val neighbors = distances.take(K)
        val labelCount = neighbors.groupingBy { it.first }.eachCount()
        val topLabel = labelCount.maxByOrNull { it.value }?.key ?: "Desconocido"
        
        // Confianza basada en la distancia del vecino más cercano (normalizada)
        val bestDistance = neighbors.first().second
        val confidence = (1.0f / (1.0f + bestDistance)).coerceIn(0f, 1f)

        return GestureResult(
            label = topLabel,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    fun close() {
        samples = emptyList()
    }
}
