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
    private val SIMILARITY_THRESHOLD = 0.80f // Ajustable: 0.8 a 0.95 suele ser ideal

    fun initialize(context: Context): Boolean {
        return try {
            // Intentar cargar desde almacenamiento interno primero (si el usuario capturó nuevas señas)
            // Si no existe, cargar el template de los assets.
            val internalFile = File(context.filesDir, "training/gestures_dataset.csv")
            val lines = if (internalFile.exists()) {
                internalFile.readLines()
            } else {
                try {
                    context.assets.open("dataset_template.csv").bufferedReader().readLines()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (lines.isEmpty()) return false
            
            samples = lines
                .drop(1) // Omitir encabezado
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size < 64) return@mapNotNull null
                        val label = parts[0].trim()
                        val rawFeatures = parts.drop(1).map { it.toFloat() }.toFloatArray()
                        
                        // REQUERIMIENTO: Ajuste de Dataset dinámico (Normalización al cargar)
                        Sample(label, normalizeLandmarks(rawFeatures))
                    } catch (e: Exception) {
                        null
                    }
                }
            samples.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * REQUERIMIENTO: Normalización de Punto Cero y Escala Unificada.
     * Esto hace que el modelo sea independiente del tamaño de la mano y la distancia.
     */
    private fun normalizeLandmarks(raw: FloatArray): FloatArray {
        val normalized = FloatArray(63)
        
        // 1. TRASLACIÓN: Restar coordenadas de la muñeca (puntos 0,1,2) a todos los demás.
        val baseX = raw[0]
        val baseY = raw[1]
        val baseZ = raw[2]

        for (i in 0 until 21) {
            normalized[i * 3] = raw[i * 3] - baseX
            normalized[i * 3 + 1] = raw[i * 3 + 1] - baseY
            normalized[i * 3 + 2] = raw[i * 3 + 2] - baseZ
        }

        // 2. ESCALA: Dividir por la distancia máxima (Distancia Euclidiana Máxima).
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

    /**
     * REQUERIMIENTO: Algoritmo de Comparación Robusto (Similitud de Coseno).
     */
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

        // Preparar entrada actual
        val inputRaw = FloatArray(63)
        handLandmarks.landmarks.take(21).forEachIndexed { index, landmark ->
            inputRaw[index * 3] = landmark.x
            inputRaw[index * 3 + 1] = landmark.y
            inputRaw[index * 3 + 2] = landmark.z
        }
        
        // Normalizar entrada con la misma lógica que el dataset
        val inputNormalized = normalizeLandmarks(inputRaw)

        // Comparar similitudes
        val similarities = samples.map { sample ->
            sample.label to cosineSimilarity(inputNormalized, sample.features)
        }.sortedByDescending { it.second }

        val bestNeighbors = similarities.take(K)
        val topMatch = bestNeighbors.first()
        
        // Aplicar umbral de confianza
        return if (topMatch.second < SIMILARITY_THRESHOLD) {
            GestureResult("Identificando...", topMatch.second, now)
        } else {
            // Votación KNN
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
