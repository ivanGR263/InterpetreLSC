package com.cesmag.nexing.ui.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.cesmag.nexing.domain.model.CameraDataset
import com.cesmag.nexing.domain.model.GestureResult
import com.cesmag.nexing.ml.KNNGestureClassifier
import com.cesmag.nexing.ml.MediaPipeHandDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraUiState(
    val recognizedText: String = "Inicializando reconocimiento...",
    val confidence: Float = 0f,
    val framesAnalyzed: Int = 0,
    val detectorReady: Boolean = false,
    val classifierReady: Boolean = false,
    val activeCamera: CameraDataset = CameraDataset.FRONT,
    val statusMessage: String = "Cámara lista",
    val environmentalTips: List<String> = emptyList(),
)

class CameraRecognitionViewModel : ViewModel() {
    private val handDetector = MediaPipeHandDetector()
    private val gestureClassifier = KNNGestureClassifier()

    private var framesWithoutHand = 0
    private var framesWithLowLight = 0
    private var appContext: Context? = null
    
    // REQUERIMIENTO: Control de tiempo para sincronización de 1.5s
    private var lastUpdateTime = 0L
    private val updateCooldownMs = 1500L

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        if (_uiState.value.detectorReady) return
        appContext = context.applicationContext

        val detectorInitialized = handDetector.initialize(context)
        val classifierInitialized = gestureClassifier.initialize(context, CameraDataset.FRONT)
        _uiState.value = _uiState.value.copy(
            detectorReady = detectorInitialized,
            classifierReady = classifierInitialized,
            activeCamera = CameraDataset.FRONT,
            statusMessage = when {
                detectorInitialized && classifierInitialized -> "MediaPipe + KNN (${CameraDataset.FRONT.displayName})"
                detectorInitialized -> "Detector listo. Falta dataset ${CameraDataset.FRONT.fileSuffix}."
                else -> "Error al inicializar detector de manos."
            }
        )
    }

    fun onCameraChanged(cameraSelector: CameraSelector) {
        val camera = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraDataset.BACK
        } else {
            CameraDataset.FRONT
        }
        if (camera == _uiState.value.activeCamera) return

        val context = appContext ?: return
        val classifierReady = gestureClassifier.switchCamera(context, camera)
        _uiState.value = _uiState.value.copy(
            activeCamera = camera,
            classifierReady = classifierReady,
            recognizedText = if (classifierReady) "Listo (${camera.displayName})" else "Dataset no disponible",
            statusMessage = if (classifierReady) {
                "Modelo cargado: ${camera.displayName}"
            } else {
                "No hay dataset para ${camera.displayName}"
            }
        )
    }

    fun onFrameAnalyzed(imageProxy: ImageProxy) {
        val nextCount = _uiState.value.framesAnalyzed + 1
        val brightness = calculateAverageBrightness(imageProxy)
        val detectedHands = handDetector.detect(imageProxy)
        val primaryHand = detectedHands.firstOrNull()
        val result: GestureResult = gestureClassifier.classify(primaryHand)

        val tips = mutableListOf<String>()

        if (brightness < 50) {
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
            if ((result.confidence < 0.6f) && (result.label != "Buscando mano...")) {
                tips.add("Seña poco clara: intenta mover un poco la mano")
            }
        }

        if (tips.size >= 2) tips.add("Sugerencia: Limpia el lente de tu cámara")

        val currentTime = System.currentTimeMillis()
        val canUpdateText = (currentTime - lastUpdateTime) > updateCooldownMs
        val isSignificantResult = result.label != "Buscando mano..." && result.label != "Identificando..."
        
        // REQUERIMIENTO: Solo actualizar el texto si ha pasado el lapso de 1.5s 
        // o si es un cambio de estado crítico (como perder la mano)
        val newRecognizedText = if (canUpdateText || !isSignificantResult) {
            if (isSignificantResult && _uiState.value.recognizedText != result.label) {
                lastUpdateTime = currentTime
            }
            result.label
        } else {
            _uiState.value.recognizedText
        }

        _uiState.value = _uiState.value.copy(
            recognizedText = newRecognizedText,
            confidence = result.confidence,
            framesAnalyzed = nextCount,
            environmentalTips = tips,
            statusMessage = if (primaryHand != null) {
                "${_uiState.value.activeCamera.displayName} · Mano ${primaryHand.handedness}"
            } else {
                "${_uiState.value.activeCamera.displayName} · Sin mano detectada"
            }
        )
    }

    private fun calculateAverageBrightness(image: ImageProxy): Double {
        val buffer = image.planes[0].buffer
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
