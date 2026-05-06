package com.umariana.lscbridge.ui.screens

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.umariana.lscbridge.data.local.GestureDatasetStore
import com.umariana.lscbridge.ml.MediaPipeHandDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminTrainingUiState(
    val currentLabel: String = "hola",
    val isCapturing: Boolean = false,
    val samplesCaptured: Int = 0,
    val detectorReady: Boolean = false,
    val statusMessage: String = "Configura etiqueta y presiona iniciar",
    val datasetPath: String = ""
)

class AdminTrainingViewModel : ViewModel() {
    private val handDetector = MediaPipeHandDetector()
    private var datasetStore: GestureDatasetStore? = null
    private var frameCount = 0

    private val _uiState = MutableStateFlow(AdminTrainingUiState())
    val uiState: StateFlow<AdminTrainingUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        if (_uiState.value.detectorReady) return
        datasetStore = GestureDatasetStore(context)
        val detectorInitialized = handDetector.initialize(context)
        _uiState.value = _uiState.value.copy(
            detectorReady = detectorInitialized,
            datasetPath = datasetStore?.datasetPath().orEmpty(),
            statusMessage = if (detectorInitialized) {
                "Detector listo. Puedes iniciar captura."
            } else {
                "No se pudo iniciar detector. Verifica hand_landmarker.task"
            }
        )
    }

    fun onLabelChange(newValue: String) {
        _uiState.value = _uiState.value.copy(currentLabel = newValue.lowercase().trim())
    }

    fun toggleCapture() {
        val shouldCapture = !_uiState.value.isCapturing
        _uiState.value = _uiState.value.copy(
            isCapturing = shouldCapture,
            statusMessage = if (shouldCapture) {
                "Capturando muestras para '${_uiState.value.currentLabel}'"
            } else {
                "Captura detenida"
            }
        )
    }

    fun onFrame(imageProxy: ImageProxy) {
        frameCount += 1
        if (!_uiState.value.isCapturing) return
        if (frameCount % 5 != 0) return

        val label = _uiState.value.currentLabel
        if (label.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Etiqueta vacia. Escribe una etiqueta.")
            return
        }

        val hand = handDetector.detect(imageProxy).firstOrNull()
        if (hand == null) {
            _uiState.value = _uiState.value.copy(statusMessage = "No se detecta mano. Ajusta encuadre.")
            return
        }

        datasetStore?.appendSample(label, hand)
        _uiState.value = _uiState.value.copy(
            samplesCaptured = _uiState.value.samplesCaptured + 1,
            statusMessage = "Muestra guardada para '$label'"
        )
    }

    override fun onCleared() {
        super.onCleared()
        handDetector.close()
    }
}
