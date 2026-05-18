package com.cesmag.nexing.ui.screens

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesmag.nexing.ui.components.HandGuidelineOverlay
import com.cesmag.nexing.util.SpeechManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraRecognitionScreen(
    viewModel: CameraRecognitionViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraExecutor: ExecutorService = remember {
        Executors.newSingleThreadExecutor()
    }
    
    // REQUERIMIENTO: Inicializar el motor de TTS de forma segura
    val speechManager = remember { SpeechManager(context) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            imageAnalysisBackpressureStrategy = STRATEGY_KEEP_ONLY_LATEST
            setImageAnalysisAnalyzer(
                cameraExecutor,
                FrameAnalyzer(onFrame = viewModel::onFrameAnalyzed)
            )
        }
    }

    // REQUERIMIENTO: Lógica de control de repetición (Cada vez que cambia el texto)
    LaunchedEffect(uiState.recognizedText) {
        speechManager.speakResult(uiState.recognizedText)
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
            cameraExecutor.shutdown()
            // REQUERIMIENTO: Liberar el motor de TTS para evitar fugas de memoria
            speechManager.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    controller = cameraController
                }
            }
        )

        // GUÍA VISUAL: Bosquejo para ubicar la mano
        HandGuidelineOverlay()

        // REQUERIMIENTO: Sugerencias ambientales (Iluminación, detección, etc.)
        EnvironmentalTipsOverlay(tips = uiState.environmentalTips)

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Gesto detectado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = uiState.recognizedText.uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confianza: ${(uiState.confidence * 100f).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Button(
                        onClick = {
                            val newSelector =
                                if (cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                }
                            cameraController.cameraSelector = newSelector
                            viewModel.onCameraChanged(newSelector)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Cambiar cámara"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Girar")
                    }
                }
                
                if (uiState.statusMessage.isNotEmpty()) {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

    }
}

@Composable
fun EnvironmentalTipsOverlay(tips: List<String>) {
    AnimatedVisibility(
        visible = tips.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp) // Debajo de la parte superior de la pantalla
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tips.forEach { tip ->
                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.width(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}

private class FrameAnalyzer(
    private val onFrame: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            onFrame(image)
        } finally {
            image.close()
        }
    }
}
