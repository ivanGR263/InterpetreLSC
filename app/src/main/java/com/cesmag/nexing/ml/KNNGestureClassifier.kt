package com.cesmag.nexing.ml

import android.content.Context
import android.util.Log
import com.cesmag.nexing.domain.model.CameraDataset
import com.cesmag.nexing.domain.model.GestureResult
import com.cesmag.nexing.domain.model.HandLandmarks
import java.io.File
import kotlin.math.sqrt

class KNNGestureClassifier {
    private class Sample(val label: String, val features: FloatArray)

    private var samples: List<Sample> = emptyList()
    private var activeCamera: CameraDataset = CameraDataset.FRONT
    private val kCount = 3
    private val similarityThreshold = 0.80f

    fun initialize(context: Context, camera: CameraDataset = CameraDataset.FRONT): Boolean {
        activeCamera = camera
        return loadSamples(context, camera)
    }

    fun switchCamera(context: Context, camera: CameraDataset): Boolean {
        if ((camera == activeCamera) && samples.isNotEmpty()) return true
        activeCamera = camera
        return loadSamples(context, camera)
    }

    private fun loadSamples(context: Context, camera: CameraDataset): Boolean {
        return try {
            val allLines = mutableListOf<String>()

            try {
                val assetLines = context.assets.open(camera.templateAssetName)
                    .bufferedReader()
                    .readLines()
                if (assetLines.isNotEmpty()) {
                    allLines.addAll(assetLines.drop(1))
                }
            } catch (e: Exception) {
                Log.e("KNN", "Error cargando asset ${camera.templateAssetName}: ${e.message}")
            }

            val internalFile = File(context.filesDir, "training/${camera.internalFileName}")
            if (internalFile.exists()) {
                val internalLines = internalFile.readLines()
                if (internalLines.size > 1) {
                    allLines.addAll(internalLines.drop(1))
                }
            }

            if (allLines.isEmpty()) return false

            samples = allLines
                .asSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size < 64) return@mapNotNull null
                        val label = parts[0].trim()
                            .replace("+", "")
                            .replace("-", "")
                            .replace("±", "")
                            .trim()
                        if (label.isEmpty() || label.lowercase() == "biblioteca") return@mapNotNull null
                        val rawFeatures = parts.asSequence().drop(1).map { it.toFloat() }.toList().toFloatArray()
                        Sample(label, normalizeLandmarks(rawFeatures))
                    } catch (_: Exception) {
                        null
                    }
                }
                .toList()

            Log.d("KNN", "Dataset ${camera.fileSuffix} cargado con ${samples.size} muestras.")
            samples.isNotEmpty()
        } catch (_: Exception) {
            false
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
                (normalized[i * 3] * normalized[i * 3]) +
                    (normalized[i * 3 + 1] * normalized[i * 3 + 1]) +
                    (normalized[i * 3 + 2] * normalized[i * 3 + 2])
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
        if (samples.isEmpty()) {
            return GestureResult("Error: Dataset ${activeCamera.fileSuffix} vacío", 0f, now)
        }
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

        val bestNeighbors = similarities.take(kCount)
        val topMatch = bestNeighbors.first()

        return if (topMatch.second < similarityThreshold) {
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
