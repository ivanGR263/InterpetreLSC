package com.umariana.lscbridge.ml

import android.content.Context
import android.util.Log
import com.umariana.lscbridge.domain.model.GestureResult
import com.umariana.lscbridge.domain.model.HandLandmarks
import java.io.File
import kotlin.math.sqrt

class KNNGestureClassifier {
    private data class Sample(val label: String, val features: FloatArray)

    private var samples: List<Sample> = emptyList()
    private val K = 3
    private val SIMILARITY_THRESHOLD = 0.80f

    fun initialize(context: Context): Boolean {
        return try {
            val allLines = mutableListOf<String>()

            // 1. CARGAR SIEMPRE EL DATASET MAESTRO (Assets)
            // Esto garantiza que el abecedario (A-Z, Ñ) siempre esté presente
            try {
                val assetLines = context.assets.open("dataset_template.csv").bufferedReader().readLines()
                if (assetLines.isNotEmpty()) {
                    allLines.addAll(assetLines.drop(1)) // Omitir header del asset
                }
            } catch (e: Exception) {
                Log.e("KNN", "Error cargando asset: ${e.message}")
            }

            // 2. CARGAR DATASET INTERNO (Si existe)
            // Esto suma las señas nuevas que el usuario haya grabado
            val internalFile = File(context.filesDir, "training/gestures_dataset.csv")
            if (internalFile.exists()) {
                val internalLines = internalFile.readLines()
                if (internalLines.size > 1) {
                    allLines.addAll(internalLines.drop(1))
                }
            }

            if (allLines.isEmpty()) return false
            
            samples = allLines
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size < 64) return@mapNotNull null
                        val label = parts[0].trim()
                        
                        // FILTRO DE SEGURIDAD: Omitir etiquetas obsoletas si se desea
                        if (label.lowercase() == "biblioteca") return@mapNotNull null

                        val rawFeatures = parts.drop(1).map { it.toFloat() }.toFloatArray()
                        Sample(label, normalizeLandmarks(rawFeatures))
                    } catch (e: Exception) {
                        null
                    }
                }
            
            Log.d("KNN", "Dataset cargado con ${samples.size} muestras.")
            samples.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Borra el archivo de entrenamiento interno para limpiar datos viejos.
     */
    fun resetInternalDataset(context: Context) {
        val internalFile = File(context.filesDir, "training/gestures_dataset.csv")
        if (internalFile.exists()) {
            internalFile.delete()
            Log.d("KNN", "Dataset interno eliminado correctamente.")
        }
    }

    private fun normalizeLandmarks(raw: FloatArray): FloatArray {
        val normalized = FloatArray(63)
        val baseX = raw[0]
        val baseY = raw[1]
        val baseZ = raw[2]

        for (i in 0 until 21) {
            normalized[i * 3] = raw[i * 3] - baseX
            normalized[i * 3 + 1] = raw[i * 3 + 1] - baseY
            normalized[i * 3 + 2] = raw[i * 3 + 2] - baseZ
        }

        var maxDist = 0f
        for (i in 0 until 21) {
            val d = sqrt(
                normalized[i * 3] * normalized[i * 3] +
                normalized[i * 3 + 1] * normalized[i * 3 + 1] +
                normalized[i * 3 + 2] * normalized[i * 3 + 2]
            )
            if (d > maxDist) maxDist = d
        }

        if (maxDist > 0) {
            for (i in normalized.indices) {
                normalized[i] /= maxDist
            }
        }
        return normalized
    }

    private fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }

    fun classify(handLandmarks: HandLandmarks?): GestureResult {
        val now = System.currentTimeMillis()
        if (samples.isEmpty()) return GestureResult("Error: Dataset vacío", 0f, now)
        if (handLandmarks == null || handLandmarks.landmarks.size < 21) {
            return GestureResult("Buscando mano...", 0f, now)
        }

        val inputRaw = FloatArray(63)
        handLandmarks.landmarks.take(21).forEachIndexed { index, landmark ->
            inputRaw[index * 3] = landmark.x
            inputRaw[index * 3 + 1] = landmark.y
            inputRaw[index * 3 + 2] = landmark.z
        }
        
        val inputNormalized = normalizeLandmarks(inputRaw)

        val similarities = samples.map { sample ->
            sample.label to cosineSimilarity(inputNormalized, sample.features)
        }.sortedByDescending { it.second }

        val bestNeighbors = similarities.take(K)
        val topMatch = bestNeighbors.first()
        
        return if (topMatch.second < SIMILARITY_THRESHOLD) {
            GestureResult("Identificando...", topMatch.second, now)
        } else {
            val finalLabel = bestNeighbors.groupingBy { it.first }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: topMatch.first
            
            GestureResult(
                label = finalLabel,
                confidence = topMatch.second,
                timestamp = now
            )
        }
    }

    fun close() {
        samples = emptyList()
    }
}
