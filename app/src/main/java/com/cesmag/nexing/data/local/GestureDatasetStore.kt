package com.cesmag.nexing.data.local

import android.content.Context
import com.cesmag.nexing.domain.model.CameraDataset
import com.cesmag.nexing.domain.model.HandLandmarks
import java.io.File
import java.util.Locale

class GestureDatasetStore(private val context: Context) {
    private val datasetDir: File = File(context.filesDir, "training").apply { mkdirs() }

    fun appendSample(label: String, hand: HandLandmarks, camera: CameraDataset) {
        if (hand.landmarks.size < 21) return
        val datasetFile = fileFor(camera)
        if (!datasetFile.exists()) {
            datasetFile.writeText(buildHeader())
        }
        datasetFile.appendText(buildRow(label = label, hand = hand))
    }

    fun datasetPath(camera: CameraDataset): String = fileFor(camera).absolutePath

    fun countSamples(camera: CameraDataset): Int {
        val file = fileFor(camera)
        if (!file.exists()) return 0
        val lines = file.readLines()
        return (lines.size - 1).coerceAtLeast(0)
    }

    fun deleteDataset(camera: CameraDataset): Boolean {
        val file = fileFor(camera)
        return !file.exists() || file.delete()
    }

    private fun fileFor(camera: CameraDataset): File =
        File(datasetDir, camera.internalFileName)

    private fun buildHeader(): String {
        val featureHeader = (0 until 63).joinToString(",") { "f$it" }
        return "label,$featureHeader\n"
    }

    private fun buildRow(label: String, hand: HandLandmarks): String {
        val values = hand.landmarks
            .take(21)
            .flatMap { listOf(it.x, it.y, it.z) }
            .joinToString(",") { value -> String.format(Locale.US, "%.6f", value) }
        return "${label.trim()},$values\n"
    }
}
