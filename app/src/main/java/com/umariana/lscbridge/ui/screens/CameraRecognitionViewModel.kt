package com.umariana.lscbridge.ui.screens

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.umariana.lscbridge.domain.model.GestureResult
import com.umariana.lscbridge.ml.MediaPipeHandDetector
import com.umariana.lscbridge.ml.TFLiteGestureClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraUiState(
    val recognizedText: String = "Inicializando reconocimiento...",
    val confidence: Float = 0f,
    val framesAnalyzed: Int = 0,
    val detectorReady: Boolean = false,
    val classifierReady: Boolean = false,
    val statusMessage: String = "Cámara lista"
)

class CameraRecognitionViewModel : ViewModel() {
    private val handDetector = MediaPipeHandDetector()
    private val gestureClassifier = TFLiteGestureClassifier()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        if (_uiState.value.detectorReady) return

        val detectorInitialized = handDetector.initialize(context)
        val classifierInitialized = gestureClassifier.initialize(context)
        _uiState.value = _uiState.value.copy(
            detectorReady = detectorInitialized,
            classifierReady = classifierInitialized,
            statusMessage = if (detectorInitialized && classifierInitialized) {
                "MediaPipe + TFLite inicializados"
            } else {
                "Faltan assets ML: hand_landmarker.task o gesture_classifier.tflite"
            }
        )
    }

    fun onFrameAnalyzed(imageProxy: ImageProxy) {
        val nextCount = _uiState.value.framesAnalyzed + 1
        val detectedHands = handDetector.detect(imageProxy)
        val primaryHand = detectedHands.firstOrNull()
        val result: GestureResult = gestureClassifier.classify(primaryHand)

        _uiState.value = _uiState.value.copy(
            recognizedText = result.label,
            confidence = result.confidence,
            framesAnalyzed = nextCount,
            statusMessage = if (primaryHand != null) {
                "Mano ${primaryHand.handedness} detectada"
            } else {
                "Sin mano detectada"
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        handDetector.close()
        gestureClassifier.close()
    }
}
