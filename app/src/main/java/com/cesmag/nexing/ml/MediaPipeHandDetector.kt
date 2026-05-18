package com.cesmag.nexing.ml

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.cesmag.nexing.domain.model.HandLandmark
import com.cesmag.nexing.domain.model.HandLandmarks

class MediaPipeHandDetector {
    private var handLandmarker: HandLandmarker? = null

    fun initialize(context: Context): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(2)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun detect(imageProxy: ImageProxy): List<HandLandmarks> {
        val detector = handLandmarker ?: return emptyList()
        return try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = detector.detectForVideo(
                mpImage,
                imageProxy.imageInfo.timestamp
            )

            val landmarksList = result.landmarks()
            val handednessList = result.handednesses()
            landmarksList.mapIndexed { index, handLandmarks ->
                val handedness = handednessList
                    .getOrNull(index)
                    ?.firstOrNull()
                    ?.categoryName()
                    ?: "Unknown"

                HandLandmarks(
                    landmarks = handLandmarks.map { landmark ->
                        HandLandmark(
                            x = landmark.x(),
                            y = landmark.y(),
                            z = landmark.z()
                        )
                    },
                    handedness = handedness
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
