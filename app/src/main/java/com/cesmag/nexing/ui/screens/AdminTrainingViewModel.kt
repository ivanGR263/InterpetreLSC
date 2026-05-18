package com.cesmag.nexing.ui.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.cesmag.nexing.data.local.GestureDatasetStore
import com.cesmag.nexing.domain.model.CameraDataset
import com.cesmag.nexing.ml.MediaPipeHandDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminTrainingUiState(
    val currentLabel: String = "hola",
    val isCapturing: Boolean = false,
    val samplesCapturedFront: Int = 0,
    val samplesCapturedBack: Int = 0,
    val activeCamera: CameraDataset = CameraDataset.FRONT,
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
        val store = datasetStore!!
        _uiState.value = _uiState.value.copy(
            detectorReady = detectorInitialized,
            samplesCapturedFront = store.countSamples(CameraDataset.FRONT),
            samplesCapturedBack = store.countSamples(CameraDataset.BACK),
            datasetPath = store.datasetPath(CameraDataset.FRONT),
            statusMessage = if (detectorInitialized) {
                "Detector listo. Dataset: ${CameraDataset.FRONT.displayName}"
            } else {
                "No se pudo iniciar detector. Verifica hand_landmarker.task"
            }
        )
    }

    fun onLabelChange(newValue: String) {
        _uiState.value = _uiState.value.copy(currentLabel = newValue.lowercase().trim())
    }

    fun onCameraChanged(cameraSelector: CameraSelector) {
        val camera = cameraSelector.toCameraDataset()
        if (camera == _uiState.value.activeCamera) return

        val wasCapturing = _uiState.value.isCapturing
        _uiState.value = _uiState.value.copy(
            activeCamera = camera,
            isCapturing = false,
            datasetPath = datasetStore?.datasetPath(camera).orEmpty(),
            statusMessage = if (wasCapturing) {
                "Captura pausada. Ahora: ${camera.displayName}"
            } else {
                "Dataset activo: ${camera.displayName}"
            }
        )
    }

    fun toggleCapture() {
        val shouldCapture = !_uiState.value.isCapturing
        val camera = _uiState.value.activeCamera
        _uiState.value = _uiState.value.copy(
            isCapturing = shouldCapture,
            statusMessage = if (shouldCapture) {
                "Capturando en ${camera.displayName} para '${_uiState.value.currentLabel}'"
            } else {
                "Captura detenida (${camera.displayName})"
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
            _uiState.value = _uiState.value.copy(
                statusMessage = "No se detecta mano. Ubícala dentro del recuadro guía."
            )
            return
        }

        val camera = _uiState.value.activeCamera
        datasetStore?.appendSample(label, hand, camera)

        when (camera) {
            CameraDataset.FRONT -> _uiState.value = _uiState.value.copy(
                samplesCapturedFront = _uiState.value.samplesCapturedFront + 1,
                statusMessage = "Muestra guardada (frontal) para '$label'"
            )
            CameraDataset.BACK -> _uiState.value = _uiState.value.copy(
                samplesCapturedBack = _uiState.value.samplesCapturedBack + 1,
                statusMessage = "Muestra guardada (trasera) para '$label'"
            )
        }
    }

    fun refreshSampleCounts() {
        val store = datasetStore ?: return
        _uiState.value = _uiState.value.copy(
            samplesCapturedFront = store.countSamples(CameraDataset.FRONT),
            samplesCapturedBack = store.countSamples(CameraDataset.BACK)
        )
    }

    fun activeCamera(): CameraDataset = _uiState.value.activeCamera

    override fun onCleared() {
        super.onCleared()
        handDetector.close()
    }
}

private fun CameraSelector.toCameraDataset(): CameraDataset =
    if (this == CameraSelector.DEFAULT_BACK_CAMERA) CameraDataset.BACK else CameraDataset.FRONT
