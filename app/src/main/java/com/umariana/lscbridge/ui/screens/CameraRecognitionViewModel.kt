package com.umariana.lscbridge.ui.screens

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.umariana.lscbridge.domain.model.GestureResult
import com.umariana.lscbridge.ml.MediaPipeHandDetector
import com.umariana.lscbridge.ml.KNNGestureClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraUiState(
    val recognizedText: String = "Inicializando reconocimiento...",
    val confidence: Float = 0f,
    val framesAnalyzed: Int = 0,
    val detectorReady: Boolean = false,
    val classifierReady: Boolean = false,
    val statusMessage: String = "Cámara lista",
    val environmentalTips: List<String> = emptyList()
)

class CameraRecognitionViewModel : ViewModel() {
    private val handDetector = MediaPipeHandDetector()
    private val gestureClassifier = KNNGestureClassifier()
    
    // Contadores para tips persistentes
    private var framesWithoutHand = 0
    private var framesWithLowLight = 0

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
                "MediaPipe + KNN (CSV) inicializados"
            } else if (detectorInitialized) {
                "Detector listo. Asegúrate de que el CSV esté en la carpeta de entrenamiento."
            } else {
                "Error al inicializar detector de manos."
            }
        )
    }

    fun onFrameAnalyzed(imageProxy: ImageProxy) {
        val nextCount = _uiState.value.framesAnalyzed + 1
        
        // 1. Analizar Iluminación (Promedio del plano Y - Luminancia)
        val brightness = calculateAverageBrightness(imageProxy)
        
        // 2. Detección de manos
        val detectedHands = handDetector.detect(imageProxy)
        val primaryHand = detectedHands.firstOrNull()
        val result: GestureResult = gestureClassifier.classify(primaryHand)

        // 3. Generar Tips Ambientales
        val tips = mutableListOf<String>()
        
        if (brightness < 50) { // Umbral de oscuridad (0-255)
            framesWithLowLight++
            if (framesWithLowLight > 15) tips.add("Mala iluminación: busca un sitio más claro")
        } else {
            framesWithLowLight = 0
        }

        if (primaryHand == null) {
            framesWithoutHand++
            if (framesWithoutHand > 20) tips.add("Mano no detectada: ubícala dentro del cuadro")
        } else {
            framesWithoutHand = 0
            if (result.confidence < 0.6f && result.label != "Buscando mano...") {
                tips.add("Seña poco clara: intenta mover un poco la mano")
            }
        }
        
        // Tip genérico si hay problemas persistentes
        if (tips.size >= 2) tips.add("Sugerencia: Limpia el lente de tu cámara")

        _uiState.value = _uiState.value.copy(
            recognizedText = result.label,
            confidence = result.confidence,
            framesAnalyzed = nextCount,
            environmentalTips = tips,
            statusMessage = if (primaryHand != null) {
                "Mano ${primaryHand.handedness} detectada"
            } else {
                "Sin mano detectada"
            }
        )
    }

    private fun calculateAverageBrightness(image: ImageProxy): Double {
        val buffer = image.planes[0].buffer // Plano Y (Luminancia)
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var total = 0L
        for (byte in data) {
            total += (byte.toInt() and 0xFF)
        }
        return total.toDouble() / data.size
    }

    override fun onCleared() {
        super.onCleared()
        handDetector.close()
        gestureClassifier.close()
    }
}
