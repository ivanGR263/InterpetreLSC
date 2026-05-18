package com.cesmag.nexing.domain.model

enum class CameraDataset(
    val fileSuffix: String,
    val displayName: String
) {
    FRONT("front", "Cámara frontal"),
    BACK("back", "Cámara trasera");

    val templateAssetName: String get() = "dataset_template_$fileSuffix.csv"
    val internalFileName: String get() = "gestures_dataset_$fileSuffix.csv"
}
