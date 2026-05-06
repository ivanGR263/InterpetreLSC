package com.umariana.lscbridge.data.local

import android.content.Context
import com.umariana.lscbridge.domain.model.HandLandmarks
import java.io.File
import java.util.Locale

class GestureDatasetStore(private val context: Context) {
    private val datasetDir: File = File(context.filesDir, "training").apply { mkdirs() }
    private val datasetFile: File = File(datasetDir, "gestures_dataset.csv")

    fun appendSample(label: String, hand: HandLandmarks) {
        if (hand.landmarks.size < 21) return
        if (!datasetFile.exists()) {
            datasetFile.writeText(buildHeader())
        }
        val row = buildRow(label = label, hand = hand)
        datasetFile.appendText(row)
    }

    fun datasetPath(): String = datasetFile.absolutePath

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
